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

package org.springframework.cloud.stream.pubsub.runtime.core;

import java.util.HashMap;
import java.util.Map;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.http.HttpMethod;
import org.springframework.integration.expression.ValueExpression;

/**
 * @author Soby Chacko
 */
public class PubSubRuntimeEnvironmentPostProcessor implements EnvironmentPostProcessor {

	//TODO: This is phase - 1. It will be refactored appropriately.
	@Override
	public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
		Map<String, Object> additionalProps = new HashMap<>();
		if (environment.containsProperty("spring.cloud.stream.pubsub.runtime.running-mode") && environment.getProperty("spring.cloud.stream.pubsub.runtime.running-mode").startsWith("SUBSCRIBER") ||
			environment.containsProperty("spring.cloud.stream.pubsub.runtime.transport-mode") && environment.getProperty("spring.cloud.stream.pubsub.runtime.transport-mode").equals("HTTP")) {
			additionalProps.put("spring.cloud.function.grpc.server", false);
		}
		else if (!environment.containsProperty("spring.cloud.stream.pubsub.runtime.transport-mode")) {
			additionalProps.put("spring.cloud.function.grpc.server", false);
		}
		if (!environment.containsProperty("spring.cloud.stream.pubsub.runtime.transport-mode")) {
			additionalProps.put("spring.cloud.stream.pubsub.runtime.transport-mode", "HTTP");
		}
		additionalProps.put("spring.cloud.stream.function.autodetect", false);

		if (environment.getProperty("spring.cloud.stream.pubsub.runtime.running-mode").startsWith("SUBSCRIBER") &&
			(!environment.containsProperty("spring.cloud.stream.pubsub.runtime.transport-mode") ||
					environment.getProperty("spring.cloud.stream.pubsub.runtime.transport-mode").equals("HTTP"))) {
			additionalProps.put("http.request.url-expression", "headers['url']");
			additionalProps.put("http.request.http-method-expression", new ValueExpression<>(HttpMethod.POST));
		}
		environment.getPropertySources().addLast(new MapPropertySource(
			"grpc.server", additionalProps));
	}
}
