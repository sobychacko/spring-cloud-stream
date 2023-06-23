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

package org.springframework.cloud.stream.pubsub.runtime.autoconfig;

import java.util.function.Consumer;
import java.util.function.Supplier;

import reactor.core.publisher.Flux;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.fn.http.request.HttpRequestFunctionConfiguration;
import org.springframework.cloud.fn.supplier.http.HttpSupplierConfiguration;
import org.springframework.cloud.function.context.FunctionCatalog;
import org.springframework.cloud.stream.config.BindingServiceProperties;
import org.springframework.cloud.stream.function.FunctionConfiguration;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.cloud.stream.function.StreamFunctionProperties;
import org.springframework.cloud.stream.pubsub.runtime.http.HttpProxyConsumer;
import org.springframework.cloud.stream.pubsub.runtime.http.HttpSinkRegistrar;
import org.springframework.cloud.stream.pubsub.runtime.http.HttpSourceRegistrar;
import org.springframework.cloud.stream.pubsub.runtime.properties.PubSubRuntimeProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.messaging.Message;

/**
 * @author Soby Chacko
 */
@AutoConfiguration
@AutoConfigureBefore({FunctionConfiguration.class})
@ConditionalOnProperty(prefix = "pubsub.runtime", name = "transport-mode", havingValue = "HTTP", matchIfMissing = true)
@EnableConfigurationProperties(PubSubRuntimeProperties.class)
public class HttpTransportAutoConfiguration {

	@Configuration(proxyBeanMethods = false)
	@ConditionalOnExpression("'${pubsub.runtime.running-mode}'.equals('PUBLISHER') && '${pubsub.runtime.transport-mode}'.equals('HTTP')")
	@Import(HttpSupplierConfiguration.class)
	public static class HttpSourceAutoConfig {

		@Bean
		public HttpSourceRegistrar httpSupplierRegister(FunctionCatalog functionCatalog, Supplier<Flux<Message<byte[]>>> httpSupplier,
														ConfigurableEnvironment configurableEnvironment, PubSubRuntimeProperties pubSubRuntimeProperties,
														StreamFunctionProperties streamFunctionProperties) {
			return new HttpSourceRegistrar(functionCatalog, httpSupplier, configurableEnvironment,
				pubSubRuntimeProperties, streamFunctionProperties);
		}
	}

	@Configuration(proxyBeanMethods = false)
	@ConditionalOnExpression("'${pubsub.runtime.running-mode}'.startsWith('SUBSCRIBER') && '${pubsub.runtime.transport-mode}'.equals('HTTP')")
	@Import(HttpRequestFunctionConfiguration.class)
	public static class HttpSinkAutoConfig {

		@Bean
		public HttpSinkRegistrar httpSinkRegistrar(FunctionCatalog functionCatalog, Consumer<byte[]> proxyConsumer,
													ConfigurableEnvironment configurableEnvironment, PubSubRuntimeProperties pubSubRuntimeProperties,
													StreamFunctionProperties streamFunctionProperties,
													BindingServiceProperties bindingServiceProperties) {
			return new HttpSinkRegistrar(functionCatalog, proxyConsumer, configurableEnvironment, pubSubRuntimeProperties,
				streamFunctionProperties, bindingServiceProperties);
		}

		@Bean
		public Consumer<byte[]> proxyConsumer(StreamBridge streamBridge, PubSubRuntimeProperties pubSubRuntimeProperties,
											HttpRequestFunctionConfiguration.HttpRequestFunction httpRequestFunction) {
			return new HttpProxyConsumer(pubSubRuntimeProperties, streamBridge, httpRequestFunction);
		}

	}
}
