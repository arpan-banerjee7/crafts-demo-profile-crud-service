package com.crafts.profileservice.producer;

import com.crafts.profileservice.config.props.KafkaPropsConfig;
import com.crafts.profileservice.exception.KafkaProcessingException;
import io.github.resilience4j.retry.annotation.Retry;
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
public class UserProfileSubmissionKafkaProducer {
    @Autowired
    private KafkaPropsConfig kafkaPropsConfig;
    private final KafkaTemplate<String, String> kafkaTemplate;

    public UserProfileSubmissionKafkaProducer(@Qualifier("userProfileSubmissionKafkaTemplate") KafkaTemplate<String, String> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    @Retry(name = "kafka-producer-retry", fallbackMethod = "sendFallback")
    public <T> void send(String message, String eventType, String key) throws KafkaProcessingException {
        String userProfileSubmissionTopic = kafkaPropsConfig.getUserProfileSubmissionTopic();
        if (null==userProfileSubmissionTopic) {
            log.error("User profile submission topic is not available ignoring message {}, Event type {}", message, eventType);
            return;
        }
        try {
            List<Header> headers = Arrays.asList(new RecordHeader("EVENT_TYPE", eventType.getBytes()), new RecordHeader("USER_ID", key.getBytes()));
            ProducerRecord<String, String> producerRecord = new ProducerRecord<>(
                    userProfileSubmissionTopic, null, null, null, message, headers);
            log.info("Sending message to Topic: {}, Event type {}", userProfileSubmissionTopic, eventType);
            kafkaTemplate.send(producerRecord);
        }catch (Exception e){
            log.info("Exception in sending message to Topic: {}, Event type {}", userProfileSubmissionTopic, eventType);
            throw new KafkaProcessingException("Error while sending message to :" + userProfileSubmissionTopic, e);
        }
    }
    // Fallback method
    public <T> void sendFallback(String message, String eventType, String key, Exception e) {
        log.error("Failed to send message after validation for userId: {}. Reason: {}", key, e.getMessage());
    }
}
