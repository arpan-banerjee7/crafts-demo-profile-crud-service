package com.crafts.profileservice.service.impl;

import com.crafts.profileservice.dto.ProductValidationStatus;
import com.crafts.profileservice.dto.UserProfileDTO;
import com.crafts.profileservice.dto.UserProfileValidationResponseDTO;
import com.crafts.profileservice.dto.UserProfileValidationResultDTO;
import com.crafts.profileservice.enums.ValidationStatusEnum;
import com.crafts.profileservice.exception.JsonDeserializationException;
import com.crafts.profileservice.exception.KafkaProcessingException;
import com.crafts.profileservice.exception.UserProfileBusinessException;
import com.crafts.profileservice.producer.UserProfileValidationResultKafkaProducer;
import com.crafts.profileservice.service.UserProfileValidationService;
import com.crafts.profileservice.util.JsonUtil;
import com.crafts.profileservice.util.ValidationUtil;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.client.circuitbreaker.ReactiveCircuitBreakerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.stream.Collectors;

import static com.crafts.profileservice.util.ValidationUtil.handleWebClientError;
import static reactor.util.retry.Retry.backoff;

@Service
@Slf4j
public class UserProfileValidationServiceImpl implements UserProfileValidationService {
    private final WebClient webClient;
    private final UserProfileValidationResultKafkaProducer userProfileValidationResultKafkaProducer;
    private final ProductRegistryServiceImpl productRegistryService;
    private final ReactiveCircuitBreakerFactory<?, ?> cbFactory;


    public UserProfileValidationServiceImpl(WebClient.Builder webClientBuilder, UserProfileValidationResultKafkaProducer userProfileValidationResultKafkaProducer, ProductRegistryServiceImpl productRegistryService, ReactiveCircuitBreakerFactory cbFactory) {
        this.webClient = webClientBuilder.build();  // no base URI set
        this.userProfileValidationResultKafkaProducer = userProfileValidationResultKafkaProducer;
        this.productRegistryService = productRegistryService;
        this.cbFactory = cbFactory;
    }

    @Override
    public void validateUserProfile(String message) {
        if (Objects.nonNull((message))) {
            UserProfileDTO userProfileDTO = null;
            try {
                userProfileDTO = JsonUtil.readValue(message, UserProfileDTO.class);
                validateUserProfile(userProfileDTO);
            } catch (JsonDeserializationException e) {
                log.error("Error in incoming User Profile message: {}", message);
                log.error("Exception occurred during user profile message consumption", e);
            }
        }
    }

    @Override
    public void validateUserProfile(UserProfileDTO userProfileDTO) throws UserProfileBusinessException {
        // get the product validation endpoints from the list of products user has subscribed to
        Map<String, String> productValidationEndpoints = userProfileDTO.getSubscriptions()
                .stream()
                .collect(Collectors.toMap(
                        productId -> productId,
                        productId -> {
                            // Retrieve the endpoint from the loaded data
                            String endpoint = productRegistryService.getProductValidationEndpoint(productId);
                            if (endpoint == null) {
                                log.error("User profile update failed for userId {}, with error: {}, doing rollback", userProfileDTO.getUserId(), "No products found for the user");
                                userProfileDTO.setConsolidatedStatus(ValidationStatusEnum.NOT_COMPLETE.getStatus()); // fallback
                                userProfileDTO.setConsolidatedMessage("Profile create/update rolled back due to missing validation endpoint from a subscribed product.");
                                sendMessageToKafka(userProfileDTO, "VALIDATION_NOT_COMPLETED");
                                throw new NoSuchElementException("Null endpoint found for productId: " + productId);
                            }
                            return endpoint;
                        }
                ));

        // make async http calls to each of the endpoints
        List<CompletableFuture<UserProfileValidationResponseDTO>> validationFutures = productValidationEndpoints.entrySet().stream()
                .map(entry -> {
                    String productId = entry.getKey();
                    String endpoint = entry.getValue();
                    return validateEndpoint(endpoint, userProfileDTO, productId);
                })
                .toList();

        validateAndSend(validationFutures, userProfileDTO);
    }

    public CompletableFuture<UserProfileValidationResponseDTO> validateEndpoint(String endpoint, UserProfileDTO userProfileDTO, String productId) {
        return webClient.post()
                .uri(endpoint)
                .header("Content-Type", "application/json")
                .body(Mono.just(userProfileDTO), UserProfileDTO.class)
                .header("productId", productId)
                .retrieve()
                .bodyToMono(UserProfileValidationResponseDTO.class)
                .onErrorResume(WebClientResponseException.class, webException -> {
                    if (webException.getStatusCode() == HttpStatus.BAD_REQUEST) {
                        // Deserialize the error response and return a DTO for BAD_REQUEST
                        return Mono.just(handleWebClientError(webException));
                    }
                    // For non-400 exceptions, propagate them further
                    return Mono.error(webException);
                })
                .transform(it -> // The circuit breaker will now only deal with non-400 exceptions
                        cbFactory.create("validate-endpoint")
                        .run(it, Mono::error))
                .toFuture();  // Convert Mono to CompletableFuture
    }


    public void validateAndSend(List<CompletableFuture<UserProfileValidationResponseDTO>> validationFutures, UserProfileDTO userProfileDTO) {
        UserProfileValidationResultDTO result = new UserProfileValidationResultDTO();
        Map<String, ProductValidationStatus> subscriptions = result.getSubscriptions();

        // Use CompletableFuture.allOf to wait for all validations to complete.
        CompletableFuture<Void> allOf = CompletableFuture.allOf(validationFutures.toArray(new CompletableFuture[0]));

        try {
            allOf.join();
        } catch (CompletionException e) {
            // One of the futures exceptionally completed, means some network error has occurred, it's not a validation failure, so rollback
            log.error("User profile update failed for userId {} due to unexpected error.", userProfileDTO.getUserId(), e);
            userProfileDTO.setConsolidatedStatus(ValidationStatusEnum.NOT_COMPLETE.getStatus()); // fallback
            userProfileDTO.setConsolidatedMessage("Could not perform profile validation due to some unexpected error from a subscribed product.");
            if(userProfileDTO.isCreateFlow()){
                userProfileDTO.setSubscriptions(new ArrayList<>());
            }
            sendMessageToKafka(userProfileDTO, "VALIDATION_NOT_COMPLETE");
            throw new RuntimeException("Validation failed due to unexpected error", e);
        }
        boolean isALlValidationSuccess = true;
        for (CompletableFuture<UserProfileValidationResponseDTO> future : validationFutures) {
            UserProfileValidationResponseDTO validationResponse = future.join(); // Extract the result

            ProductValidationStatus productStatus = ValidationUtil.getProductValidationStatus(validationResponse);
            if (productStatus.getStatus().equals("Rejected")) {
                isALlValidationSuccess = false;
            }
            subscriptions.put(validationResponse.getProductId(), productStatus);
        }
        result.setUserId(userProfileDTO.getUserId());
        if (isALlValidationSuccess) {
            log.info("Validation success for user with userId {}", userProfileDTO.getUserId());
            userProfileDTO.setConsolidatedStatus(ValidationStatusEnum.SUCCESS.getStatus());
            userProfileDTO.setConsolidatedMessage("User profile validated by all subscribed products");
            // add the newly subscribed product to the list
            if (null != userProfileDTO.getExistingSubscriptions() && !userProfileDTO.getExistingSubscriptions().isEmpty()) {
                userProfileDTO.getSubscriptions().addAll(userProfileDTO.getExistingSubscriptions());
                userProfileDTO.getSubscriptionValidations().putAll(result.getSubscriptions());
            } else {
                userProfileDTO.setSubscriptionValidations(result.getSubscriptions());
            }
            sendMessageToKafka(userProfileDTO, "USER_PROFILE_VALIDATED");
        } else {
            log.error("Validation failed for user with userId {}", userProfileDTO.getUserId());
            if (null != userProfileDTO.getExistingSubscriptions() && !userProfileDTO.getExistingSubscriptions().isEmpty()) {
                userProfileDTO.getSubscriptionValidations().putAll(result.getSubscriptions());
            } else {
                userProfileDTO.setSubscriptionValidations(result.getSubscriptions());
            }
            UserProfileDTO rejectedUserProfileDTO = getFailureUserProfileDTO(userProfileDTO);
            sendMessageToKafka(rejectedUserProfileDTO, "USER_PROFILE_REJECTED");
        }
    }

    private static UserProfileDTO getFailureUserProfileDTO(UserProfileDTO userProfileDTO) {
        UserProfileDTO rejectedUserProfileDTO = new UserProfileDTO();
        if(userProfileDTO.isCreateFlow()){
            rejectedUserProfileDTO.setSubscriptions(new ArrayList<>());
        }
        rejectedUserProfileDTO.setUserId(userProfileDTO.getUserId());
        rejectedUserProfileDTO.setConsolidatedStatus(ValidationStatusEnum.REJECTED.getStatus());
        rejectedUserProfileDTO.setSubscriptionValidations(userProfileDTO.getSubscriptionValidations());
        rejectedUserProfileDTO.setConsolidatedMessage("One or more subscribed products did not approve the profile update.");
        return rejectedUserProfileDTO;
    }

    private void sendMessageToKafka(UserProfileDTO userProfileDTO, String eventType) {
        String jsonMessage = JsonUtil.writeToJson(userProfileDTO);
        log.info("Sending message to kafka with user details {}", jsonMessage);
        try {
            userProfileValidationResultKafkaProducer.send(jsonMessage, eventType, userProfileDTO.getUserId());
        } catch (KafkaProcessingException e) {
            log.error("Failed to send message for userId: {}. Reason: {}", userProfileDTO.getUserId(), e.getMessage());
            throw new RuntimeException(e);
        }
    }

}
