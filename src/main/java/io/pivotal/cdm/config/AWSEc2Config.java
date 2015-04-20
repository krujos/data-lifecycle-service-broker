package io.pivotal.cdm.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.amazonaws.services.ec2.AmazonEC2Client;

@Configuration
class AWSEc2Config {

	@Value("#{environment.AWS_S3_ACCESS_KEY}")
	private String accessKey;

	@Value("#{environment.AWS_S3_SECRET_KEY}")
	private String secretKey;

	@Bean(destroyMethod = "shutdown")
	public AmazonEC2Client ec2Client() {
		return new AmazonEC2Client();
	}
}
