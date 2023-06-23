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

package org.springframework.cloud.stream.pubsub.runtime.http;

import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

import reactor.core.publisher.Flux;

import org.springframework.cloud.function.context.FunctionCatalog;
import org.springframework.cloud.function.context.FunctionRegistration;
import org.springframework.cloud.function.context.FunctionRegistry;
import org.springframework.cloud.function.context.catalog.FunctionTypeUtils;
import org.springframework.cloud.stream.function.BindableFunctionProxyFactory;
import org.springframework.cloud.stream.function.StreamFunctionProperties;
import org.springframework.cloud.stream.pubsub.runtime.core.AbstractFunctionsRegistrar;
import org.springframework.cloud.stream.pubsub.runtime.properties.PubSubRuntimeProperties;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.messaging.Message;
import org.springframework.util.StringUtils;

/**
 * @author Soby Chacko
 */
public class HttpSourceRegistrar extends AbstractFunctionsRegistrar {

	private final FunctionCatalog functionCatalog;

	private final Supplier<Flux<Message<byte[]>>> httpSupplier;

	private final ConfigurableEnvironment configurableEnvironment;

	private final PubSubRuntimeProperties pubSubRuntimeProperties;

	private final StreamFunctionProperties streamFunctionProperties;

	public HttpSourceRegistrar(FunctionCatalog functionCatalog, Supplier<Flux<Message<byte[]>>> httpSupplier,
							ConfigurableEnvironment configurableEnvironment, PubSubRuntimeProperties pubSubRuntimeProperties,
							StreamFunctionProperties streamFunctionProperties) {
		this.functionCatalog = functionCatalog;
		this.httpSupplier = httpSupplier;
		this.configurableEnvironment = configurableEnvironment;
		this.pubSubRuntimeProperties = pubSubRuntimeProperties;
		this.streamFunctionProperties = streamFunctionProperties;
	}

	@Override
	public void afterPropertiesSet() {
		if (this.pubSubRuntimeProperties.getRunningMode() == PubSubRuntimeProperties.RunningMode.PUBLISHER) {
			Map<String, Object> springCloudStreamProperties = new HashMap<>();

			String publisherDestination = this.pubSubRuntimeProperties.getPublisher().getDestination();
			if (StringUtils.hasText(publisherDestination)) {
				springCloudStreamProperties.put(SPRING_CLOUD_STREAM_BINDINGS + "httpSupplier-out-0.destination", publisherDestination);
			}
			this.configurableEnvironment.getPropertySources().addLast(new MapPropertySource(
				"spring.cloud.stream.bindings", springCloudStreamProperties));

			Type functionType = FunctionTypeUtils.discoverFunctionType(this.httpSupplier, "httpSupplier", (GenericApplicationContext) this.getApplicationContext());
			((FunctionRegistry) this.functionCatalog).register(new FunctionRegistration<>(httpSupplier, "httpSupplier").type(functionType));

			AtomicReference<BindableFunctionProxyFactory> proxyFactory = new AtomicReference<>();
			proxyFactory.set(new BindableFunctionProxyFactory("httpSupplier",
				0, 1, this.streamFunctionProperties));
			// Concern - multiple suppliers? How do we handle if things need to go to multiple topics
			((GenericApplicationContext) this.getApplicationContext()).registerBean("httpSupplier" + "_binding",
				BindableFunctionProxyFactory.class, () -> proxyFactory.get());
		}
	}
}
