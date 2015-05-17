package org.cloudfoundry.community.servicebroker.datalifecycle.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.core.task.TaskExecutor;

@Configuration
class TaskExecutorConfig {

	@Bean
	public TaskExecutor newTaskExecutor() {
		return new SimpleAsyncTaskExecutor();
	}
}
