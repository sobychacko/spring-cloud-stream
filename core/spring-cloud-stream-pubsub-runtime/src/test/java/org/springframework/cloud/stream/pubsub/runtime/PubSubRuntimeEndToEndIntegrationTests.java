/*
 * Copyright 2023-2023 the original author or authors.
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

package org.springframework.cloud.stream.pubsub.runtime;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.cloud.stream.binder.test.InputDestination;
import org.springframework.cloud.stream.binder.test.OutputDestination;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.FormHttpMessageConverter;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.messaging.Message;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(SpringExtension.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class PubSubRuntimeEndToEndIntegrationTests {

	static CountDownLatch latch = new CountDownLatch(1);

	@Test
	void sourceIntegrationTest() {
		try (ConfigurableApplicationContext context = new SpringApplicationBuilder(AdditionalConfig.class)
			.run(
				"--server.port=0",
				"--pubsub.runtime.running-mode=PUBLISHER",
				"--pubsub.runtime.publisher.destination=foobar")) {
			String port = context.getEnvironment().getProperty("local.server.port");
			RestTemplate restTemplate = context.getBean(RestTemplate.class);
			String toHttpEndpoint = "http://localhost:" + port;
			restTemplate.postForObject(toHttpEndpoint, new HttpEntity<>("HELLO SCSt RUNTIME!!"), ResponseEntity.class);
			OutputDestination outputDestination = context.getBean(OutputDestination.class);
			Message<byte[]> outputMessage = outputDestination.receive(0, "foobar.destination");
			assertThat(new String(outputMessage.getPayload(), StandardCharsets.UTF_8)).isEqualTo("HELLO SCSt RUNTIME!!");
		}
	}

	@Test
	void sinkIntegrationTest() throws InterruptedException {
		try (ConfigurableApplicationContext context = new SpringApplicationBuilder(AdditionalConfig.class, TestController.class)
			.run(
				"--server.port=8084",
				"--pubsub.runtime.running-mode=SUBSCRIBER",
				"--pubsub.runtime.subscriber.destination=foobar",
				"--pubsub.runtime.subscriber.invokable-endpoint=http://localhost:8084/passthrough")) {
			InputDestination inputDestination = context.getBean(InputDestination.class);
			Message<byte[]> inputMessage = MessageBuilder.withPayload("Hello".getBytes()).build();
			inputDestination.send(inputMessage, "foobar.destination");
			Assert.isTrue(latch.await(5, TimeUnit.SECONDS), "Failed to receive message");
		}
	}

	@EnableAutoConfiguration
	static class AdditionalConfig {

		@Bean
		public RestTemplate restTemplate() {
			RestTemplate restTemplate = new RestTemplate();
			restTemplate.getMessageConverters().add(new FormHttpMessageConverter());
			return restTemplate;
		}
	}

	@RestController
	public static class TestController {

		@PostMapping("/passthrough")
		public HttpStatus passthrough(@RequestBody String data) {
			assertThat(data).isEqualTo("Hello");
			latch.countDown();
			return HttpStatus.ACCEPTED;
		}
	}
}
