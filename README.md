#Test Env Setup
This builds a postgres environment for testing. 

- Create a VPC
- Create a new ubuntu machine (t1.micro is fine and free) 
- apt-get update
- Follow the [postgres install instructions](https://help.ubuntu.com/community/PostgreSQL)
- apt-get install postgressql
- change the listen_address to * in /etc/postgresql/<version>/main/postgresql.conf
- allow remote connections to pg_hba.conf
- host    all             all             0.0.0.0/0               md5
- connect w/ plsql or pgadmin and add some test data.


#Running
Access to AWS is done by using keys for an IAM user. The IAM key needs full EC2 access. The application code relies on AWS_ACCESS_KEY_ID and AWS_SECRET_ACCESS_KEY being set to the respective values for this user. 

#Notes
The approach where we take a snap of prod, then make an AMI, then mount it etc may be a little heavy handed. It works for a demo and POC, but in reality we should be leveraging snaps that are created on some other schedule. 


#How the service broker works
##Catalog
The catalog endpoint offers up two plans. <code>Production</code> works as any normal brokered connection to a database does. Credentials are inserted to your environment and then you modify production data. <code>Copy</code> is slightly more devious. This will provision a new VM and copy of the database from a snapshot. The credentials are then inserted, but the data and virtual machine live only as long as the binding lives. 

##Provision
If `plan_id` is set to `prod`, provision doesn't do much of anything. 

Upon a provision call with `plan_id` set to `copy`, the we will call into AWS to create an AMI from the running instance. What you say? The database is not quiesced? You are correct. #1, As stated in the notes section, in production we should probably leverage a snap that was created via some other means (like a backup), so that system is hopefully insuring good snaps. #2, your database is crash consistent, or you should stop whatever you're doing and get one that is. Either way, it's probably  not be that big of a deal for this use case as getting the last few seconds of transactions isn't all that import for test data. Depends on the workload & YMMV. Like anything, don't blindly grab code off the internet. Anyway, we create the AMI, and then start an instance from that AMI. We pass the coordinates to the new instance upon bind. There's lots of artifacts that get created along the way by AWS. A snapshot, a volume, an instance. Upon deprovision, we clean them all up. 


##Bind
Binding simply dumps the credentials for the copy into ```VCAP_SERVICES```. 

#Deployment
The broker needs a place to store it's data about running instances (so it can deprovision them). Currently we assume the broker is running in a CloudFoundry instance. Provision a service from the marketplace for the broker to store it's persistent data. I've used postgres, but anything that exposes a `DataSource` that [Spring Cloud](http://projects.spring.io/spring-cloud/) understands should work. Here's the [docs](http://docs.cloudfoundry.org/buildpacks/java/spring-service-bindings.html#rdbms). Below I use the free plan in PWS, you probably want something else. 

```
cf create-service elephantsql turtle lifecycle-db 
```

Set the following environment variables, This example uses postgres as the production database and AWS as the copy provider. If you're implementing a different `CopyProvider` or `DataProvider` you may need to set slightly different environment variables based on their requirements. 

```
export AWS_ACCESS_KEY_ID=<AWS ACCESS KEY>
export AWS_SECRET_ACCESS_KEY=<AWS SECRET KEY>
export SECURITY_USER_NAME=user
export SECURITY_USER_PASSWORD=password
export PROD_DB_PASSWORD=postgres
export PROD_DB_USER=postgres
export PROD_DB_URI postgres://$PROD_DB_USER:$PROD_DB_PASSWORD@10.10.10.10:5432/testdb
export SECURITY_USER_NAME=user
export SECURITY_USER_PASSWORD=pass
export SOURCE_INSTANCE_ID=i-xxxxxxx
export SUBNET_ID=subnet-XXXXXX
```

After you've exported all that push the app from the same directory as `manifest.yml`. The manifest assumes you've created a provisioned a database instance from the marketplace and named it `lifecycle-sb-db`. 

```
cf push --no-start
```

Now setup all the environment exported

```
./set-cf-env.sh
```

Now start it.

```
cf start lifecycle-sb
```


#Console
There's a console that gives you an overview of what the service broker is doing running at ```<sb_url>/```. So if you deployed to ```http://broker.cfapps.io/``` you would see the console at that address. The console is primitive, but it does tell you what EC2 instances are running and you can track backwards to find resources if needed. Eventually it will show inflight operations too, as those can be a bit hard to track.

#Tests
This project separates unit and integration tests by using the maven surefire and failsafe plugins. The integration tests need environment variables described in the deployment section.

From the command line: 

```
	$ mvn test # unit tests
	$ mvn integration-test #integration tests
```

Eclipse / Intellij targets can be setup by pointing at ```src/test/java/io/pivotal/cdm/UnitTests.java``` and ```src/test/java/io/pivotal/cdm/IntegrationTests.java``` respectively. Integration tests will need the environment variables setup to function properly. 

The username and password are passed the the service broker api calls. Both the broker and test source these out of the environment, so you can use whatever you want for both values. The AWS variables should come from an IAM user. These tests will start and stop VM's, as well as create and delete snapshots and AMI's in AWS. 

There's probably some limitations around subnet. I've only tested deploying into the same VPC as prod. It also probably only works in US-EAST, again untested.

##Adding tests
Unit tests are simple, just add one. Integration tests need to have the jUnit category annotation pointing at the spring boot integration test class. This annotation value is then used to tell the test suites what to run. The maven config uses surefire or failsafe to point at the suites.

For example:

```java
	@RunWith(SpringJUnit4ClassRunner.class)
	@SpringApplicationConfiguration(classes = CdmServiceBrokerApplication.class)
	@WebAppConfiguration
	@IntegrationTest("server.port:0")
	@Category(IntegrationTest.class)
	public class CatalogIntegrationTest {
	}
```

There's tests suites that search for the presence, or lack of the annotation to decide which kind of test it is. In eclipse I have run configurations for each test suite. 

#Code Scructure

Copies are provisioned through the `CopyProvider` interface. Create an implementation and inject it via a configuration class. 

##Internal Components
The `LCServiceInstanceService` and `LCServiceInstanceBindingService` store their data in Spring Data Repository classes. In order to make these easier to test and to make the code cleaner they interact with the repositories via `LCServiceInstanceManager` and `LCServiceInstanceBindingManager` classes. The implementations of the manager classes assume that a `DataSource` will be injected via Spring auto config. 