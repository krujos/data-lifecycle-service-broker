package io.pivotal.cdm.config;

import io.pivotal.cdm.aws.AWSCopyProvider;
import io.pivotal.cdm.aws.AWSHelper;
import io.pivotal.cdm.provider.CopyProvider;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.amazonaws.services.ec2.AmazonEC2Client;

@Configuration
public class AWSCopyProviderConfig {

	@Value("#{environment.PROD_DB_USER}")
	private String username;
	@Value("#{environment.PROD_DB_PASSWORD}")
	private String password;
	@Value("#{environment.PROD_DB_URI}")
	private String uri;
	@Value("#{environment.SOURCE_INSTANCE_ID}")
	private String sourceInstance;
	@Value("#{environment.SUBNET_ID}")
	private String subnetId;
	@Autowired
	private AmazonEC2Client ec2Client;

	@Bean
	CopyProvider copyProvider() {
		return new AWSCopyProvider(new AWSHelper(ec2Client, subnetId,
				sourceInstance), username, password, uri, sourceInstance);

	}
}
