/*
 * Copyright 2019-2022 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.cloud.stream.binder.tck;

import java.util.function.Function;

import org.junit.jupiter.api.Test;

import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.cloud.stream.binder.test.InputDestination;
import org.springframework.cloud.stream.binder.test.TestChannelBinderConfiguration;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.GenericMessage;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Oleg Zhurakousky
 *
 */
public class ErrorHandlingTests {

	@Test
	void testGlobalErrorWithMessage() {
		ApplicationContext context = new SpringApplicationBuilder(GlobalErrorHandlerWithErrorMessageConfig.class)
			.web(WebApplicationType.NONE).run("--spring.jmx.enabled=false");
		InputDestination source = context.getBean(InputDestination.class);
		source.send(new GenericMessage<>("foo".getBytes()));
		GlobalErrorHandlerWithErrorMessageConfig config = context
			.getBean(GlobalErrorHandlerWithErrorMessageConfig.class);
		assertThat(config.globalErroInvoked).isTrue();
	}

	@Test
	void testGlobalErrorWithThrowable() {
		ApplicationContext context = new SpringApplicationBuilder(GlobalErrorHandlerWithThrowableConfig.class)
			.web(WebApplicationType.NONE).run("--spring.jmx.enabled=false");
		InputDestination source = context.getBean(InputDestination.class);
		source.send(new GenericMessage<>("foo".getBytes()));
		GlobalErrorHandlerWithThrowableConfig config = context.getBean(GlobalErrorHandlerWithThrowableConfig.class);
		assertThat(config.globalErroInvoked).isTrue();
	}

	@Import(TestChannelBinderConfiguration.class)
	@EnableAutoConfiguration
	public static class GlobalErrorHandlerWithErrorMessageConfig {

		private boolean globalErroInvoked;

		@Bean
		public Function<String, String> func() {
			return v -> {
				throw new RuntimeException("test exception");
			};
		}

		@ServiceActivator(inputChannel = "errorChannel")
		public void generalError(Message<?> message) {
			this.globalErroInvoked = true;
		}

	}

	@Import(TestChannelBinderConfiguration.class)
	@EnableAutoConfiguration
	public static class GlobalErrorHandlerWithThrowableConfig {

		private boolean globalErroInvoked;

		@Bean
		public Function<String, String> func() {
			return v -> {
				throw new RuntimeException("test exception");
			};
		}

		@ServiceActivator(inputChannel = "errorChannel")
		public void generalError(Throwable exception) {
			this.globalErroInvoked = true;
		}

	}

}
