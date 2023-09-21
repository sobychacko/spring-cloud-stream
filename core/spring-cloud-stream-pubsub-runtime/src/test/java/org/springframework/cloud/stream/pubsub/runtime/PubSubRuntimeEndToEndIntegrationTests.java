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
import java.util.Objects;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import com.fasterxml.jackson.databind.ObjectMapper;
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
				"--spring.cloud.stream.pubsub.runtime.running-mode=PUBLISHER",
				"--spring.cloud.stream.pubsub.runtime.publisher.destination=foobar")) {
			var port = context.getEnvironment().getProperty("local.server.port");
			var restTemplate = context.getBean(RestTemplate.class);
			var toHttpEndpoint = "http://localhost:" + port;
			restTemplate.postForObject(toHttpEndpoint, new HttpEntity<>("HELLO SCSt RUNTIME!!"), ResponseEntity.class);
			var outputDestination = context.getBean(OutputDestination.class);
			Message<byte[]> outputMessage = outputDestination.receive(10, "foobar.destination");
			assertThat(new String(outputMessage.getPayload(), StandardCharsets.UTF_8)).isEqualTo("HELLO SCSt RUNTIME!!");
		}
	}

	@Test
	void sourceIntegrationTestWithPojo() throws Exception {
		try (ConfigurableApplicationContext context = new SpringApplicationBuilder(AdditionalConfig.class)
			.run(
				"--server.port=0",
				"--spring.cloud.stream.pubsub.runtime.running-mode=PUBLISHER",
				"--spring.cloud.stream.pubsub.runtime.publisher.destination=foobar")) {
			var port = context.getEnvironment().getProperty("local.server.port");
			var restTemplate = context.getBean(RestTemplate.class);
			var toHttpEndpoint = "http://localhost:" + port;
			var testPojo = new TestPojo();
			testPojo.setName("foobar");
			restTemplate.postForObject(toHttpEndpoint, new HttpEntity<>(testPojo), ResponseEntity.class);
			var outputDestination = context.getBean(OutputDestination.class);
			Message<byte[]> outputMessage = outputDestination.receive(10, "foobar.destination");
			ObjectMapper objectMapper = new ObjectMapper();
			TestPojo testPojoReceived = objectMapper.readValue(outputMessage.getPayload(), TestPojo.class);
			assertThat(testPojoReceived).isEqualTo(testPojo);
		}
	}

	@Test
	void sinkIntegrationTest() throws InterruptedException {
		try (ConfigurableApplicationContext context = new SpringApplicationBuilder(AdditionalConfig.class, TestController.class)
			.run(
				"--server.port=8084",
				"--spring.cloud.stream.pubsub.runtime.running-mode=SUBSCRIBER",
				"--spring.cloud.stream.pubsub.runtime.subscriber.destination=foobar",
				"--spring.cloud.stream.pubsub.runtime.subscriber.invokable-endpoint=http://localhost:8084/passthrough")) {
			var inputDestination = context.getBean(InputDestination.class);
			Message<byte[]> inputMessage = MessageBuilder.withPayload("Hello".getBytes()).build();
			inputDestination.send(inputMessage, "foobar.destination");
			Assert.isTrue(latch.await(5, TimeUnit.SECONDS), "Failed to receive message");
		}
	}

	@Test
	void sinkIntegrationTestPojo() throws Exception {
		try (ConfigurableApplicationContext context = new SpringApplicationBuilder(AdditionalConfig.class, TestController.class)
			.run(
				"--server.port=8084",
				"--spring.cloud.stream.pubsub.runtime.running-mode=SUBSCRIBER",
				"--spring.cloud.stream.pubsub.runtime.subscriber.destination=foobar",
				"--http.request.headers-expression={'Content-Type':'application/json'}",
				"--spring.cloud.stream.pubsub.runtime.subscriber.invokable-endpoint=http://localhost:8084/passthrough-pojo")) {
			var inputDestination = context.getBean(InputDestination.class);
			var testPojo = new TestPojo();
			testPojo.setName("foobar");
			var objectMapper = new ObjectMapper();
			byte[] bytes = objectMapper.writeValueAsBytes(testPojo);
			Message<byte[]> inputMessage = MessageBuilder.withPayload(bytes).build();
			inputDestination.send(inputMessage, "foobar.destination");
			Assert.isTrue(latch.await(5, TimeUnit.SECONDS), "Failed to receive message");
		}
	}

	@EnableAutoConfiguration
	static class AdditionalConfig {

		@Bean
		public RestTemplate restTemplate() {
			var restTemplate = new RestTemplate();
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

		@PostMapping("/passthrough-pojo")
		public HttpStatus passthroughPojo(@RequestBody TestPojo data) {
			var testPojo = new TestPojo();
			testPojo.setName("foobar");
			assertThat(data).isEqualTo(testPojo);
			latch.countDown();
			return HttpStatus.ACCEPTED;
		}
	}

	private static class TestPojo {

		private String name;

		TestPojo() {
		}

		TestPojo(String name) {
			this.name = name;
		}

		public String getName() {
			return this.name;
		}

		public void setName(String name) {
			this.name = name;
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (!(o instanceof TestPojo testPojo)) return false;
			return Objects.equals(getName(), testPojo.getName());
		}

		@Override
		public int hashCode() {
			return Objects.hash(getName());
		}
	}
}
