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

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

import org.springframework.cloud.fn.http.request.HttpRequestFunctionConfiguration;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.cloud.stream.pubsub.runtime.properties.PubSubRuntimeProperties;
import org.springframework.core.log.LogAccessor;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.util.StringUtils;

/**
 * @author Soby Chacko
 */
public class HttpProxyConsumer implements Consumer<byte[]> {

	private final LogAccessor logger = new LogAccessor(this.getClass());

	private final PubSubRuntimeProperties pubSubRuntimeProperties;

	private final StreamBridge streamBridge;

	private final HttpRequestFunctionConfiguration.HttpRequestFunction httpRequestFunction;

	public HttpProxyConsumer(PubSubRuntimeProperties pubSubRuntimeProperties,
							StreamBridge streamBridge, HttpRequestFunctionConfiguration.HttpRequestFunction httpRequestFunction) {
		this.pubSubRuntimeProperties = pubSubRuntimeProperties;
		this.streamBridge = streamBridge;
		this.httpRequestFunction = httpRequestFunction;
	}

	@Override
	public void accept(byte[] bytes) {

		PubSubRuntimeProperties.Subscriber subscriber = pubSubRuntimeProperties.getSubscriber();
		String invokableEndpointUrl = subscriber.getInvokableEndpoint();

		Map<String, Object> headers = new HashMap<>();
		headers.put("url", invokableEndpointUrl);
		Message<byte[]> message = MessageBuilder.withPayload(bytes).copyHeaders(headers).build();

		Object body = this.httpRequestFunction.apply(message);
		if (body != null) {
			HttpProxyConsumer.this.logger.debug("[Response Body] " + body);
			if (pubSubRuntimeProperties.getRunningMode() == PubSubRuntimeProperties.RunningMode.SUBSCRIBER_PUBLISHER &&
				StringUtils.hasText(subscriber.getSendToDestination())) {
				System.out.println("SENDING RESPONSE DATA TO: " + subscriber.getSendToDestination());
				streamBridge.send("streamBridge-out-0", body);
				System.out.println("SENT TO STREAM-BRIDGE");
			}
		}
	}
}

