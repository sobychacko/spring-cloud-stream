/*
 * Copyright 2015-2017 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.cloud.stream.provisioning;

import org.springframework.cloud.stream.binder.ConsumerProperties;
import org.springframework.cloud.stream.binder.ProducerProperties;

/**
 * Provisioning SPI that allows the users to provision destinations such as queues and topics.
 * This SPI will allow the binders to be separated from any provisioning concerns and only focus
 * on setting up endpoints for sending/receiving messages.
 *
 * Implementations must implement the following methods:
 *
 * <ul>
 * <li>{@link #createProducerDestinationIfNecessary(String, ProducerProperties)}</li>
 * <li>{@link #createConsumerDestinationIfNecessary(String, String, ConsumerProperties)} </li>
 * </ul>
 *
 * @param <C>  the consumer properties type
 * @param <CD> the consumer destination type
 * @param <PD> the producer destination type
 *
 * @author Soby Chacko
 *
 * @since 1.2
 */
public interface ProvisioningProvider<C extends ConsumerProperties, P extends ProducerProperties, CD, PD> {

	/**
	 * Creates target destinations for outbound channels. The implementation
	 * is middleware-specific.
	 *
	 * @param name       the name of the producer destination
	 * @param properties producer properties
	 */
	PD createProducerDestinationIfNecessary(String name, P properties);

	/**
	 * Creates the middleware destination the consumer will start to consume data from.
	 *
	 * @param name       the name of the destination
	 * @param group      the consumer group
	 * @param properties consumer properties
	 * @return reference to the consumer destination
	 */
	CD createConsumerDestinationIfNecessary(String name, String group, C properties);

}
