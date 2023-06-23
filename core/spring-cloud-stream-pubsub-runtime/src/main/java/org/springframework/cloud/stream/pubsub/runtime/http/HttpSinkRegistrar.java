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
import java.util.function.Consumer;

import org.springframework.cloud.function.context.FunctionCatalog;
import org.springframework.cloud.function.context.FunctionRegistration;
import org.springframework.cloud.function.context.FunctionRegistry;
import org.springframework.cloud.function.context.catalog.FunctionTypeUtils;
import org.springframework.cloud.stream.config.BindingProperties;
import org.springframework.cloud.stream.config.BindingServiceProperties;
import org.springframework.cloud.stream.function.BindableFunctionProxyFactory;
import org.springframework.cloud.stream.function.StreamFunctionProperties;
import org.springframework.cloud.stream.pubsub.runtime.core.AbstractFunctionsRegistrar;
import org.springframework.cloud.stream.pubsub.runtime.properties.PubSubRuntimeProperties;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.util.StringUtils;


public class HttpSinkRegistrar extends AbstractFunctionsRegistrar {

	private final FunctionCatalog functionCatalog;

	private final Consumer<byte[]> proxyConsumer;

	private final ConfigurableEnvironment configurableEnvironment;

	private final PubSubRuntimeProperties pubSubRuntimeProperties;

	private final StreamFunctionProperties streamFunctionProperties;

	private final BindingServiceProperties bindingServiceProperties;

	public HttpSinkRegistrar(FunctionCatalog functionCatalog, Consumer<byte[]> proxyConsumer, ConfigurableEnvironment configurableEnvironment,
							PubSubRuntimeProperties pubSubRuntimeProperties, StreamFunctionProperties streamFunctionProperties,
							BindingServiceProperties bindingServiceProperties) {
		this.functionCatalog = functionCatalog;
		this.proxyConsumer = proxyConsumer;
		this.configurableEnvironment = configurableEnvironment;
		this.pubSubRuntimeProperties = pubSubRuntimeProperties;
		this.streamFunctionProperties = streamFunctionProperties;
		this.bindingServiceProperties = bindingServiceProperties;
	}

	@Override
	public void afterPropertiesSet() {
		if (isSubscriber()) {
			Map<String, Object> springCloudStreamProperties = new HashMap<>();

			String subscriptionDestination = this.pubSubRuntimeProperties.getSubscriber().getDestination();
			if (StringUtils.hasText(subscriptionDestination)) {
				springCloudStreamProperties.put(SPRING_CLOUD_STREAM_BINDINGS + "proxyConsumer-in-0.destination", subscriptionDestination);
			}
			String sendToDestination = this.pubSubRuntimeProperties.getSubscriber().getSendToDestination();
			if (StringUtils.hasText(sendToDestination)) {
				springCloudStreamProperties.put(SPRING_CLOUD_STREAM_BINDINGS + "streamBridge-out-0.destination", sendToDestination);
			}
			this.configurableEnvironment.getPropertySources().addLast(new MapPropertySource(
				"spring.cloud.stream.bindings", springCloudStreamProperties));

			//TODO - In the case of source, the following bindingServiceProperties change is not needed.
			//TODO - It is only configured after this auto-config. But in the case of sink, we need to override the bean.
			BindingProperties bindingProperties = new BindingProperties();
			bindingProperties.setDestination(subscriptionDestination);
			Map<String, BindingProperties> bindingPropertiesMap = new HashMap<>();
			bindingPropertiesMap.put("proxyConsumer-in-0", bindingProperties);
			if (StringUtils.hasText(sendToDestination)) {
				BindingProperties sendToBindingProperties = new BindingProperties();
				sendToBindingProperties.setDestination(sendToDestination);
				bindingPropertiesMap.put("streamBridge-out-0", sendToBindingProperties);
			}
			bindingServiceProperties.setBindings(bindingPropertiesMap);

			Type functionType = FunctionTypeUtils.discoverFunctionType(this.proxyConsumer, "proxyConsumer", (GenericApplicationContext) this.getApplicationContext());
			((FunctionRegistry) this.functionCatalog).register(new FunctionRegistration<>(proxyConsumer, "proxyConsumer").type(functionType));

			AtomicReference<BindableFunctionProxyFactory> proxyFactory = new AtomicReference<>();
			proxyFactory.set(new BindableFunctionProxyFactory("proxyConsumer",
				1, 0, this.streamFunctionProperties));

			((GenericApplicationContext) this.getApplicationContext()).registerBean("proxyConsumer" + "_binding",
				BindableFunctionProxyFactory.class, () -> proxyFactory.get());
		}
	}

	private boolean isSubscriber() {
		PubSubRuntimeProperties.RunningMode runningMode = this.pubSubRuntimeProperties.getRunningMode();
		return runningMode == PubSubRuntimeProperties.RunningMode.SUBSCRIBER ||
			runningMode == PubSubRuntimeProperties.RunningMode.SUBSCRIBER_PUBLISHER;
	}

}

