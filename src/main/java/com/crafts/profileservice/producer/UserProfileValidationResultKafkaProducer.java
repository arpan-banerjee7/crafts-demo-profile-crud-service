package com.crafts.profileservice.producer;

import com.crafts.profileservice.config.props.KafkaPropsConfig;
import com.crafts.profileservice.exception.KafkaProcessingException;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.header.Header;
import org.apache.kafka.common.header.internals.RecordHeader;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

@Component
@Slf4j
public class UserProfileValidationResultKafkaProducer {
    @Autowired
    private KafkaPropsConfig kafkaPropsConfig;
    private final KafkaTemplate<String, String> kafkaTemplate;

    public UserProfileValidationResultKafkaProducer(@Qualifier("userProfileValidationResultKafkaTemplate") KafkaTemplate<String, String> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public <T> void send(String message, String eventType, String key) throws KafkaProcessingException {
        String userProfileValidationResultTopic = kafkaPropsConfig.getUserProfileValidationResultTopic();
        if (null == userProfileValidationResultTopic) {
            log.error("user-profile-validation-result topic is not available ignoring message {}, Event type {}", message, eventType);
            return;
        }
        try {
            List<Header> headers = Arrays.asList(new RecordHeader("EVENT_TYPE", eventType.getBytes()), new RecordHeader("USER_ID", key.getBytes()));
            ProducerRecord<String, String> producerRecord = new ProducerRecord<>(
                    userProfileValidationResultTopic, null, null, null, message, headers);
            log.info("Sending message to Topic: {}, Event type {}", userProfileValidationResultTopic, eventType);
            kafkaTemplate.send(producerRecord);
        } catch (Exception e) {
            log.info("Exception in message to Topic: {}, Event type {}", userProfileValidationResultTopic, eventType);
            throw new KafkaProcessingException("Error while sending message to :" + userProfileValidationResultTopic, e);
        }
    }
}
