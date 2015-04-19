package io.pivotal.cdm.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.core.task.TaskExecutor;

@Configuration
public class TaskExecutorConfig {

	@Bean
	public TaskExecutor newTaskExecutor() {
		return new SimpleAsyncTaskExecutor();
	}
}
