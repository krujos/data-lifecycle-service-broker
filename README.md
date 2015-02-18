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
The approach where we take a snap of prod, then make an ami, then mount it etc may be a little heavy handed. It works for a demo and POC, but in reality we should be leveraging snaps that are created on some other schedule. 



#How the service broker works
##Catalog
The catalog endpoint offers up two plans. <code>Production</code> works as any normal brokered connection to a database does. Credentials are inserted to your environment and then you modify production data. <code>Copy</code> is slightly more devious. This will provision a new VM and copy of the database from a snapshot. The credentials are then inserted, but the data and virtual machine live only as long as the binding lives. 

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

#Tests
This project separates unit and integration tests by using the maven surefire and failsafe plugins. The integration tests need environment variables described in the deployment section.

From the command line: 

```
	$ mvn test # unit tests
	$ mvn integration-test #integration tests
```

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

