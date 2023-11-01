package com.crafts.profileservice.consumer;

import com.crafts.profileservice.config.props.KafkaPropsConfig;
import com.crafts.profileservice.service.UserProfileValidationService;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Component
@Slf4j
public class UserProfileSubmissionKafkaConsumer {
    @Autowired
    private KafkaPropsConfig kafkaPropsConfig;
    @Autowired
    private UserProfileValidationService userProfileValidationService;

    @KafkaListener(
            topics = "#{kafkaPropsConfig.getUserProfileSubmissionTopic()}",
            groupId = "#{kafkaPropsConfig.getUpsConsumerGroupId()}",
            batch = "true",
            concurrency = "2")
    public void consume(ConsumerRecords<String, String> messages) {
        ExecutorService executorService = Executors.newFixedThreadPool(3);
        for (ConsumerRecord<String, String> message : messages) {
            log.info("Received message from Topic: {}, Message: {}", message.topic(), message.value());

            CompletableFuture.runAsync(
                            () -> userProfileValidationService.validateUserProfile(message.value()), executorService)
                    .exceptionally(ex -> {
                        log.error("Failed to save validation result to DB for message: {}. Error: {}", message.value(), ex.getMessage());
                        return null;
                    });

        }
    }
}
