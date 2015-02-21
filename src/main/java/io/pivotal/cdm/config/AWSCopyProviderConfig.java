package io.pivotal.cdm.config;

import io.pivotal.cdm.aws.*;
import io.pivotal.cdm.provider.CopyProvider;

import org.springframework.beans.factory.annotation.*;
import org.springframework.context.annotation.*;

import com.amazonaws.services.ec2.AmazonEC2Client;

@Configuration
public class AWSCopyProviderConfig {

	@Value("#{environment.PG_USER}")
	private String username;
	@Value("#{environment.PG_PASSWORD}")
	private String password;
	@Value("#{environment.PG_URI}")
	private String uri;
	@Value("#{environment.SOURCE_INSTANCE_ID}")
	private String sourceInstance;
	@Value("#{environment.SUBNET_ID}")
	private String subnetId;
	@Autowired
	private AmazonEC2Client ec2Client;

	@Bean
	CopyProvider copyProvider() {
		return new AWSCopyProvider(new AWSHelper(ec2Client, subnetId),
				username, password, uri, sourceInstance);

	}
}
