#LifeCycle Service Brokers
This repo contains the base elements for life cycle service brokers. `CopyProvider` and `DataProvider` implementors should include this project in their dependencies. Folks who wish to consume providers or write new ones should checkout the [lifecycle-broker-provider](https://github.com/lifecycle-broker-providers) org on github!

An example implementation for Postgres on AWS can be found [here](https://github.com/lifecycle-broker-providers/postgres-aws-broker).

##How to extend and build your own Broker?
Start a new project, include the `CopyProvider` and `DataProvider` of your choosing and write some tests. After you're satisfied that everything works run the following: 

```
cf push <your app> --no-start
cf bind-service <your app> lifecycle-sb-db #This needs to be some RDBMS.
#You must be admin for the next commands.
cf create-service-broker lifecycle-sb $SECURITY_USER_NAME $SECURITY_USER_PASSWORD <your app url>
cf enable-service-access lifecycle-sb
```

The framework needs a database to save it's state (bindings, provisioned services etc), so that's why you have to bind a service. We bind based on name. The framework has been tested with postgres and MySQL. Anything `spring-data-jpa` should work fine if you're running the broker in CloudFoundry. You can add a spring data repository as you see fit if you have additional information to store. A `DataSource` object will be injected for you.

##What the framework does for you.
The framework spins up controllers (protected with basic auth) for you, it also handles all of the asynchronous processing. So implement your copy providers in a single thread, as we'll be creating new ones for you, this also means your providers should be thread safe! The framework also keeps track of what copies are exposed. It does this by relating `instanceId`'s to each other. If you need to save additional information besides `instanceId` create a new [`Repository`](http://docs.spring.io/spring-data/commons/docs/current/api/org/springframework/data/repository/Repository.html) and go for it.

##What do you need to do for the framework.
You have to provide an implementation of a `CopyProvider` and `DataProvider`. Either of these can be no ops, but you must provide an implementation. This project uses Spring Security, so you need to set `SECURITY_USER_NAME` and `SECURITY_USER_PASSWORD` environment variables (`cf set-env`). This will translate to the values you feed to `cf create-service-broker`.

#How the service broker works
##Catalog
The catalog endpoint offers up two plans. `Production` works as any normal brokered connection to a database does. The broker injects credentials into your environment and then you change production data. `Copy` is more devious. This provisions a new VM and copy of the database from a snapshot. The credentials are then inserted, but the data and virtual machine live only as long as the binding lives. 

##Provision
If `plan_id` is set to `prod`, provision doesn't do much of anything. 

Upon a provision call with `plan_id` set to `copy`, the we will call into AWS to create an AMI from the running instance. What you say? The database is not quiesced? You are correct. #1, As stated in the notes section, in production we should leverage a snap that's created via some other means (like a backup). It is that system's responsibility to ensure a good snapshot. #2, your database is crash consistent, or you should stop whatever you're doing and get one that is. Either way, it's not that big of a deal for this use case as getting the last few seconds of transactions isn't all that import for test data. Depends on the workload & YMMV. Like anything, don't grab code off the internet. Anyway, we create the AMI, and then start an instance from that AMI. We pass the coordinates to the new instance upon bind. There's lots of artifacts that get created along the way by AWS. A snapshot, a volume, an instance. Upon deprovision, we clean them all up. 

##Bind
Binding dumps the credentials for the copy into ```VCAP_SERVICES```. 

#Console
There's a console that lets you enter the sanitize script at ```<sb_url>/```. So if you deployed to ```http://broker.cfapps.io/``` you would see the console at that address. The console is primitive, and I need a better strategy for consuming that script (Point to a repo?). 

There are some endpints to tell you what EC2 instances are running and you can track backwards to find resources if needed. They also see inflight operations. Take a peek at the [`/main/java/io/pivotal/cdm/controller/StatusController.java`](https://github.com/krujos/data-lifecycle-service-broker/blob/master/src/main/java/io/pivotal/cdm/controller/StatusController.java)

#Deployment
The broker needs a place to store it's data about running instances (so it can deprovision them). Currently we assume the broker is running in a CloudFoundry instance. Provision a service from the marketplace for the broker to store it's persistent data. I've used postgres, but anything that exposes a `DataSource` that [Spring Cloud](http://projects.spring.io/spring-cloud/) supports will work. Here's the [docs](http://docs.cloudfoundry.org/buildpacks/java/spring-service-bindings.html#rdbms). Below I use the free plan in PWS, you want something else for production. 

```
cf create-service elephantsql turtle lifecycle-db 
```

Set the following environment variables. This example uses postgres as the production database and AWS as the copy provider. If you're implementing your own `CopyProvider` or `DataProvider` you will set environment variables suitable for your use case. 

#Tests
This project separates unit and integration tests by using the maven surefire and failsafe plugins. The integration tests need environment variables described in the deployment section.

From the command line: 

```
	$ mvn test # unit tests
	$ mvn integration-test #integration tests
```

Eclipse / Intellij targets can be setup by pointing at ```src/test/java/org/cloudfoundry/community/servicebroker/datalifecycle/UnitTests.java``` and ```src/test/java/org/cloudfoundry/community/servicebroker/datalifecycle/IntegrationTests.java```. Integration tests will need the environment variables setup to function. 

The broker API needs a username and password to service API calls. Both the broker and test source these out of the environment, so you can use whatever you want for both values. The AWS variables should come from an IAM user. These tests will start and stop VM's, as well as create and delete snapshots and AMI's in AWS. 

I suspect there are limitations around subnet. I've only tested deploying into the same VPC as prod. I've only tested in US-EAST.

##Adding tests
Unit tests are simple, just add one. Integration tests use the jUnit category annotation. It points at the spring boot integration test class. This annotation value is then used to tell the test suites what to run. The maven config uses surefire or failsafe to point at the suites.

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

There's tests suites that search for the presence, or lack of the annotation to decide which kind of test it is. In eclipse I have run configuration for each. The integration configuration needs the envornment variables mentioned above to run.

#TODO
There's a tracker [here](https://www.pivotaltracker.com/n/projects/1275196). My current priority list is something along the lines of: 
*Break out providers into seperate repos
*Implement a vSphere copy provider


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
Access to AWS via an IAM user. The IAM key needs full EC2 access. The broker relies on AWS_ACCESS_KEY_ID and AWS_SECRET_ACCESS_KEY being set. Find those keys when you create the user. 

#Notes
The approach where we take a snap of prod, then make an AMI, then mount it etc may be a little heavy handed. It works for a demo and POC, but in reality we should be leveraging snaps that created by some other schedule. 

Also, this broker assumes a pool of free elastic IP's that it can pull from. If `describeAddresses` comes back with nothing we're in trouble. 

#Runnign
```
export AWS_ACCESS_KEY_ID=<AWS ACCESS KEY>
export AWS_SECRET_ACCESS_KEY=<AWS SECRET KEY>
export PROD_DB_PASSWORD=postgres
export PROD_DB_USER=postgres
export PROD_DB_URI postgresql://10.10.10.10:5432/testdb
export SECURITY_USER_NAME=user
export SECURITY_USER_PASSWORD=pass
export SOURCE_INSTANCE_ID=i-xxxxxxx
export SUBNET_ID=subnet-XXXXXX
export BOOT_CHECK_PORT=5432
```

`BOOT_CHECK_PORT` is the port that we will try to open a socket connection to to see if the machine is up and booted. If it responds on this port we hand the copy off and say it's good to go. `5432` is the default postgres port, make sure your security groups allow ingress on this port!


After you've exported all that push the app from the same directory as `manifest.yml`. The manifest assumes you've provisioned a database instance from the marketplace and named it `lifecycle-sb-db`. 

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

