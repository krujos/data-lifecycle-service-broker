package io.pivotal.cdm.config;

import org.springframework.context.annotation.*;
import org.springframework.core.task.*;

@Configuration
public class TaskExecutorConfig {

	@Bean
	public TaskExecutor newTaskExecutor() {
		return new SimpleAsyncTaskExecutor();
	}
}
