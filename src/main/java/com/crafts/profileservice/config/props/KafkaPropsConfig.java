package com.crafts.profileservice.config.props;

import lombok.Getter;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.stereotype.Component;

@Component
@RefreshScope
@Getter
@Setter
public class KafkaPropsConfig {
    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    @Value("${spring.kafka.ups.consumer.group-id}")
    private String upsConsumerGroupId;

    @Value("${spring.kafka.upv.consumer.group-id}")
    private String upvConsumerGroupId;

    @Value("${spring.kafka.consumer.auto-offset-reset}")
    private String consumerAutoOffsetReset;

    @Value("${spring.kafka.consumer.key-deserializer}")
    private String consumerKeyDeserializer;

    @Value("${spring.kafka.consumer.value-deserializer}")
    private String consumerValueDeserializer;

    @Value("${spring.kafka.consumer.max.poll.records}")
    private String consumerMaxPollRecords;

    @Value("${spring.kafka.producer.key-serializer}")
    private String producerKeySerializer;

    @Value("${spring.kafka.producer.value-serializer}")
    private String producerValueSerializer;

    @Value("${spring.kafka.user.profile.submission.topic}")
    private String userProfileSubmissionTopic;

    @Value("${spring.kafka.user.profile.validation.result.topic}")
    private String userProfileValidationResultTopic;

}
