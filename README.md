#Test Env Setup
- Create a VPC
- Create a new ubuntu machine (t1.micro is fine) 
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
If ```plan_id``` is set to prod, provision doesn't do much of anything. 

Upon a provision call with ```plan_id``` set to copy, the we will call into AWS to create an AMI from the running instance. What you say? The database and system are not quiesced? You are correct. #1, As stated in the notes section, in production we should probably leverage a snap that was created as part of a backup, so it's a non issue. #2, your database is crash consistent, it might not be that big of a deal. Depends on the workload. Anyway, we create the AMI, and then start an instance from that AMI. We pass the coordinates to the new instance upon bind. There's lots of artifacts that get created along the way by AWS. A snapshot, a volume, an instance. Upon deprovision, we clean them all up. 


##Bind
Binding simply dumps the credentials for the copy into ```VCAP_SERVICES```. 

#Deployment
Set the following environment variables. 
```
	 SECURITY_USER_NAME  #service broker username
	 SECURITY_USER_PASSWORD #service broker password
	 AWS_ACCESS_KEY_ID
	 AWS_SECRET_ACCESS_KEY
	 PG_USER #postgres user
	 PG_PASSWORD #postgres password
	 PG_URI #the postgres connection uri
	 SUBNET_ID #the was subnet to deploy copy instances into.
	 SOURCE_INSTANCE_ID #the AWS source instance ID.
	 
```

The username and password are passed the the service broker api calls. Both the broker and test source these out of the environment, so you can use whatever you want for both values. The AWS variables should come from an IAM user. These tests will start and stop VM's, as well as create and delete snapshots and AMI's in AWS. 

There's probably some limitations around subnet. I've only tested deploying into the same VPC as prod. It also probably only works in US-EAST, again untested.

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

