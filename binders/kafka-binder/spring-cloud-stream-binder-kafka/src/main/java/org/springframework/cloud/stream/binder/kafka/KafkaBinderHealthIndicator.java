/*
 * Copyright 2016-2023 the original author or authors.
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

package org.springframework.cloud.stream.binder.kafka;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.kafka.common.PartitionInfo;

import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.Status;
import org.springframework.cloud.stream.binder.kafka.common.AbstractKafkaBinderHealthIndicator;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.listener.AbstractMessageListenerContainer;
import org.springframework.scheduling.concurrent.CustomizableThreadFactory;

/**
 * Health indicator for Kafka.
 *
 * @author Ilayaperumal Gopinathan
 * @author Marius Bogoevici
 * @author Henryk Konsek
 * @author Gary Russell
 * @author Laur Aliste
 * @author Soby Chacko
 * @author Vladislav Fefelov
 * @author Chukwubuikem Ume-Ugwa
 * @author Taras Danylchuk
 */
public class KafkaBinderHealthIndicator extends AbstractKafkaBinderHealthIndicator {

	private final ExecutorService executor = Executors.newSingleThreadExecutor(
		new CustomizableThreadFactory("kafka-binder-health-"));

	private final KafkaMessageChannelBinder binder;


	public KafkaBinderHealthIndicator(KafkaMessageChannelBinder binder,
									ConsumerFactory<?, ?> consumerFactory) {
		super(consumerFactory);
		this.binder = binder;
	}

	@Override
	protected ExecutorService createHealthBinderExecutorService() {
		return Executors.newSingleThreadExecutor(
			new CustomizableThreadFactory("kafka-binder-health-"));
	}

	protected Health buildTopicsHealth() {
		try {
			initMetadataConsumer();
			Set<String> downMessages = new HashSet<>();
			Set<String> checkedTopics = new HashSet<>();
			final Map<String, KafkaMessageChannelBinder.TopicInformation> topicsInUse = KafkaBinderHealthIndicator.this.binder
					.getTopicsInUse();
			if (topicsInUse.isEmpty()) {
				try {
					this.metadataConsumer.listTopics(Duration.ofSeconds(this.timeout));
				}
				catch (Exception e) {
					return Health.down().withDetail("No topic information available",
							"Kafka broker is not reachable").build();
				}
				return Health.unknown().withDetail("No bindings found",
						"Kafka binder may not be bound to destinations on the broker").build();
			}
			else {
				for (String topic : topicsInUse.keySet()) {
					KafkaMessageChannelBinder.TopicInformation topicInformation = topicsInUse
							.get(topic);
					if (!topicInformation.isTopicPattern()) {
						List<PartitionInfo> partitionInfos = this.metadataConsumer
								.partitionsFor(topic);
						for (PartitionInfo partitionInfo : partitionInfos) {
							if (topicInformation.getPartitionInfos()
									.contains(partitionInfo)
									&& partitionInfo.leader() == null ||
									(partitionInfo.leader() != null && partitionInfo.leader().id() == -1)) {
								downMessages.add(partitionInfo.toString());
							}
							else if (this.considerDownWhenAnyPartitionHasNoLeader &&
									partitionInfo.leader() == null || (partitionInfo.leader() != null && partitionInfo.leader().id() == -1)) {
								downMessages.add(partitionInfo.toString());
							}
						}
						checkedTopics.add(topic);
					}
					else {
						try {
							// Since destination is a pattern, all we are doing is just to make sure that
							// we can connect to the cluster and query the topics.
							this.metadataConsumer.listTopics(Duration.ofSeconds(this.timeout));
						}
						catch (Exception ex) {
							return Health.down()
								.withDetail("Cluster not connected",
									"Destination provided is a pattern, but cannot connect to the cluster for any verification")
								.build();
						}
					}
				}
			}
			if (downMessages.isEmpty()) {
				return Health.up().withDetail("topicsInUse", checkedTopics).build();
			}
			else {
				return Health.down()
						.withDetail("Following partitions in use have no leaders: ",
								downMessages.toString())
						.build();
			}
		}
		catch (Exception ex) {
			return Health.down(ex).build();
		}
	}

	protected Health buildBinderSpecificHealthDetails() {
		List<AbstractMessageListenerContainer<?, ?>> listenerContainers = binder.getKafkaMessageListenerContainers();
		if (listenerContainers.isEmpty()) {
			return Health.unknown().build();
		}

		Status status = Status.UP;
		List<Map<String, Object>> containersDetails = new ArrayList<>();

		for (AbstractMessageListenerContainer<?, ?> container : listenerContainers) {
			Map<String, Object> containerDetails = new HashMap<>();
			boolean isRunning = container.isRunning();
			boolean isOk = container.isInExpectedState();
			if (!isOk) {
				status = Status.DOWN;
			}
			containerDetails.put("isRunning", isRunning);
			containerDetails.put("isStoppedAbnormally", !isRunning && !isOk);
			containerDetails.put("isPaused", container.isContainerPaused());
			containerDetails.put("listenerId", container.getListenerId());
			containerDetails.put("groupId", container.getGroupId());

			containersDetails.add(containerDetails);
		}
		return Health.status(status)
				.withDetail("listenerContainers", containersDetails)
				.build();
	}
}
