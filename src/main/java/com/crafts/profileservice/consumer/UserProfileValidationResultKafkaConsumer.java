package com.crafts.profileservice.consumer;

import com.crafts.profileservice.config.props.KafkaPropsConfig;
import com.crafts.profileservice.dto.UserProfileDTO;
import com.crafts.profileservice.service.UserProfileService;
import com.crafts.profileservice.service.UserProfileValidationService;
import com.crafts.profileservice.util.JsonUtil;
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
public class UserProfileValidationResultKafkaConsumer {

    @Autowired
    private KafkaPropsConfig kafkaPropsConfig;

    @Autowired
    private UserProfileService userProfileService;

    @KafkaListener(
            topics = "#{kafkaPropsConfig.getUserProfileValidationResultTopic()}",
            groupId = "#{kafkaPropsConfig.getUpvConsumerGroupId()}",
            batch = "true",
            concurrency = "2")
    public void consume(ConsumerRecords<String, String> messages) {
        ExecutorService executorService = Executors.newFixedThreadPool(3);
        for (ConsumerRecord<String, String> message : messages) {
            log.info("Received message from Topic: {}, Message: {}", message.topic(), message.value());
            CompletableFuture.runAsync(() -> {
                        UserProfileDTO userProfileDTO = JsonUtil.readValue(message.value(), UserProfileDTO.class);
                        userProfileService.updateAfterValidation(userProfileDTO);
                    }, executorService)
                    .exceptionally(ex -> {
                        log.error("Failed to save validation result to DB for message: {}. Error: {}", message.value(), ex.getMessage());
                        return null;
                    });
            
        }
    }
}

