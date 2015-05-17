package org.cloudfoundry.community.servicebroker.datalifecycle.config;

import org.cloudfoundry.community.servicebroker.datalifecycle.aws.AWSCopyProvider;
import org.cloudfoundry.community.servicebroker.datalifecycle.aws.AWSHelper;
import org.cloudfoundry.community.servicebroker.datalifecycle.provider.CopyProvider;
import org.cloudfoundry.community.servicebroker.datalifecycle.utils.HostUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.amazonaws.services.ec2.AmazonEC2Client;

@Configuration
class AWSCopyProviderConfig {

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
	@Autowired
	private HostUtils hostUtils;

	@Value("#{environment.BOOT_CHECK_PORT}")
	private int bootCheckPort;

	@Bean
	CopyProvider copyProvider() {
		return new AWSCopyProvider(new AWSHelper(ec2Client, subnetId,
				sourceInstance, hostUtils, bootCheckPort), username, password,
				uri, sourceInstance);

	}
}
