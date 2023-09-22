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

package org.springframework.cloud.stream.pubsub.bridge.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for the PubSub bridge platform.
 *
 * @author Soby Chacko
 */
@ConfigurationProperties("spring.cloud.stream.pubsub.bridge")
public class PubSubBridgeProperties {

	/**
	 * Publisher specific properties. See {@link Publisher}.
	 */
	private Publisher publisher = new Publisher();

	/**
	 * Subscriber specific properties. See {@link Subscriber}.
	 */
	private Subscriber subscriber = new Subscriber();

	/**
	 * The transport protocol used for application communication to the bridge.
	 * Default is HTTP.
	 */
	private TransportMode transportMode = TransportMode.HTTP;

	/**
	 * Running mode of the bridge (publisher, subscriber, both subscriber and publisher).
	 */
	private RunningMode runningMode;

	public RunningMode getRunningMode() {
		return this.runningMode;
	}

	public void setRunningMode(RunningMode runningMode) {
		this.runningMode = runningMode;
	}

	public TransportMode getTransportMode() {
		return transportMode;
	}

	public void setTransportMode(TransportMode transportMode) {
		this.transportMode = transportMode;
	}

	public Publisher getPublisher() {
		return publisher;
	}

	public void setPublisher(Publisher publisher) {
		this.publisher = publisher;
	}

	public Subscriber getSubscriber() {
		return subscriber;
	}

	public void setSubscriber(Subscriber subscriber) {
		this.subscriber = subscriber;
	}

	/**
	 * Specify Transport protocols.
	 */
	public enum TransportMode {

		/**
		 * In this mode, the bridge provides HTTP based bridge.
		 */
		HTTP,

		/**
		 * gRPC based bridge.
		 */
		GRPC;
	}

	public enum RunningMode {

		/**
		 * Publisher.
		 */
		PUBLISHER,

		/**
		 * Subscriber.
		 */
		SUBSCRIBER,

		/**
		 * Subscribe and then publish after processing.
		 */
		SUBSCRIBER_PUBLISHER;

	}

	/**
	 * Publisher properties.
	 */
	public static class Publisher {
		/**
		 * Destination on the middleware broker.
		 */
		String destination;

		public String getDestination() {
			return destination;
		}

		public void setDestination(String destination) {
			this.destination = destination;
		}
	}

	/**
	 * Subscriber properties.
	 */
	public static class Subscriber {

		/**
		 * Destination on the middleware broker.
		 */
		String destination;

		/**
		 * Outbound destination in the case of subscriber-publisher.
		 */
		String sendToDestination;

		/**
		 * Endpoint to invoke when the subscription receives data.
		 */
		String invokableEndpoint;

		/**
		 * Number of retries.
		 */
		int retriesOnError = 3;

		/**
		 * Where to send the data in case of non-recoverable errors.
		 */
		String dltDestination = "error.dlt.destination";

		public String getDestination() {
			return destination;
		}

		public void setDestination(String destination) {
			this.destination = destination;
		}

		public String getSendToDestination() {
			return this.sendToDestination;
		}

		public void setSendToDestination(String sendToDestination) {
			this.sendToDestination = sendToDestination;
		}

		public String getInvokableEndpoint() {
			return invokableEndpoint;
		}

		public void setInvokableEndpoint(String invokableEndpoint) {
			this.invokableEndpoint = invokableEndpoint;
		}

		public int getRetriesOnError() {
			return retriesOnError;
		}

		public void setRetriesOnError(int retriesOnError) {
			this.retriesOnError = retriesOnError;
		}

		public String getDltDestination() {
			return dltDestination;
		}

		public void setDltDestination(String dltDestination) {
			this.dltDestination = dltDestination;
		}
	}

}
