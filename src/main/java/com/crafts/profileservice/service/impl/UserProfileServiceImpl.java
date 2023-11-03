package com.crafts.profileservice.service.impl;

import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.crafts.profileservice.constans.ProfileServiceCache;
import com.crafts.profileservice.dto.ProductValidationStatus;
import com.crafts.profileservice.dto.SubscriptionRequestDTO;
import com.crafts.profileservice.dto.UserProfileDTO;
import com.crafts.profileservice.dto.UserProfileValidationResultDTO;
import com.crafts.profileservice.entity.UserProfileEO;
import com.crafts.profileservice.enums.ValidationStatusEnum;
import com.crafts.profileservice.exception.KafkaProcessingException;
import com.crafts.profileservice.exception.UserProfileBusinessException;
import com.crafts.profileservice.exception.UserProfileRepositoryException;
import com.crafts.profileservice.mapper.UserProfileMapper;
import com.crafts.profileservice.mapper.UserProfileMapperHelper;
import com.crafts.profileservice.producer.UserProfileSubmissionKafkaProducer;
import com.crafts.profileservice.repository.impl.UserProfileRepositoryImpl;
import com.crafts.profileservice.service.UserProfileService;
import com.crafts.profileservice.util.JsonUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;

@Service
@Slf4j
public class UserProfileServiceImpl implements UserProfileService {

    private final UserProfileRepositoryImpl userProfileRepository;
    private final UserProfileMapper userProfileMapper;
    private final UserProfileSubmissionKafkaProducer userProfileSubmissionKafkaProducer;

    public UserProfileServiceImpl(UserProfileRepositoryImpl userProfileRepository, UserProfileMapper userProfileMapper, UserProfileSubmissionKafkaProducer userProfileSubmissionKafkaProducer) {
        this.userProfileRepository = userProfileRepository;
        this.userProfileMapper = userProfileMapper;
        this.userProfileSubmissionKafkaProducer = userProfileSubmissionKafkaProducer;
    }

    @Override
    @Cacheable(value = ProfileServiceCache.USER_PROFILE_CACHE, key = "#userId", unless = "#result == null")
    public UserProfileDTO getUserProfileById(String userId) {
        try {
            UserProfileDTO userProfileDTO = userProfileMapper.convertEOtoDTO(
                    userProfileRepository.getUserProfileById(userId));
            if (Objects.isNull(userProfileDTO)) {
                throw new NoSuchElementException("No user profile found for user ID: " + userId);
            }
            return userProfileDTO;
        } catch (UserProfileRepositoryException e) {
            throw new UserProfileBusinessException("Error while retrieving user profile.", e);
        }
    }

    @Override
    public UserProfileDTO saveUserProfile(UserProfileDTO userProfileDTO) {
        try {
            if (userProfileDTO.getSubscriptions().isEmpty()) {
                throw new IllegalArgumentException("User should be subscribed to at least one product");
            }
            // Generate idempotency key based on user details
            String idempotencyKey = generateIdempotencyKey(userProfileDTO.getEmail(), userProfileDTO.getTaxIdentifiers().getPan(), userProfileDTO.getLegalName());

            // Check if the user profile already exists with the generated idempotency key
            if (userProfileRepository.existsByIdempotencyKey(idempotencyKey)) {
                // If the user profile exists, it's a duplicate request. Log and return the existing user profile.
                log.info("Duplicate request detected with idempotency key {}. User profile already exists.", idempotencyKey);
                throw new UserProfileBusinessException("Duplicate entry detected. User already exists");
            }
            userProfileDTO.setIdempotencyKey(idempotencyKey);
            userProfileDTO.setConsolidatedStatus(ValidationStatusEnum.IN_PROGRESS.getStatus());
            UserProfileEO userProfileEO = userProfileMapper.convertDTOTOEO(userProfileDTO);
            log.info("Saving user details with initial subscription status as IN_PROGRESS");
            userProfileEO = userProfileRepository.save(userProfileEO);
            UserProfileDTO savedUserDTO = userProfileMapper.convertEOtoDTO(userProfileEO);
            savedUserDTO.setCreateFlow(true);
            sendMessageToKafka(savedUserDTO, "USER_PROFILE_CREATE");
            return savedUserDTO;
        } catch (KafkaProcessingException e) {
            log.error("Failed to send message to kafka.", e);
            throw new UserProfileBusinessException("Failed to send message to kafka to proceed with further validations");
        } catch (UserProfileRepositoryException e) {
            throw new UserProfileBusinessException("Error while saving user profile.", e);
        } catch (NoSuchAlgorithmException e) {
            throw new UserProfileBusinessException("Unable to generate idempotency key due to missing algorithm.", e);
        }
    }

    @Override
    public void update(String userId, UserProfileDTO userProfileDTO) throws UserProfileBusinessException {
        try {
            UserProfileDTO saveStatusDTO = new UserProfileDTO();
            saveStatusDTO.setUserId(userId);
            saveStatusDTO.setConsolidatedStatus(ValidationStatusEnum.IN_PROGRESS.getStatus());
            UserProfileEO saveStatusEO = userProfileMapper.convertDTOTOEO(saveStatusDTO);
            log.info("Updating status for user {}", userId);
            userProfileRepository.update(userId, saveStatusEO);
            sendMessageToKafka(userProfileDTO, "USER_PROFILE_UPDATE");
        } catch (KafkaProcessingException e) {
            log.error("Failed to send message to kafka.", e);
            throw new UserProfileBusinessException("Failed to send message to kafka to proceed with further validations");
        } catch (UserProfileRepositoryException e) {
            log.error("Failed to update user data with userId {} ", userId, e);
            throw new UserProfileBusinessException("Error while updating user profile.", e);
        }
    }

    @Override
    public void addSubscription(String userId, SubscriptionRequestDTO subscriptionRequestDTO) {
        try {
            UserProfileDTO userProfileDTO = getUserProfileById(userId);
            if (userProfileDTO.getConsolidatedStatus().equals(ValidationStatusEnum.REJECTED.getStatus())) {
                log.error("Error while subscribing to this product, as user profile validation is rejected");
                throw new UserProfileBusinessException("Error while subscribing to this product, as user profile validation is rejected");
            }
            if (null != userProfileDTO.getSubscriptions()) {
                if (!userProfileDTO.getSubscriptions().isEmpty() && userProfileDTO.getSubscriptions().contains(subscriptionRequestDTO.getProductId())) {
                    log.error("Error: User has already subscribed to this product");
                    throw new UserProfileBusinessException("Error: User has already subscribed to this product");
                }
                // run validations only for the current product
                userProfileDTO.setConsolidatedStatus(ValidationStatusEnum.IN_PROGRESS.getStatus());
                userProfileDTO.setExistingSubscriptions(new ArrayList<>(userProfileDTO.getSubscriptions()));
                userProfileDTO.getSubscriptions().clear();
                userProfileDTO.getSubscriptions().add(subscriptionRequestDTO.getProductId());
                sendMessageToKafka(userProfileDTO, "USER_PROFILE_ADD_SUBSCRIPTION");
            }
        } catch (KafkaProcessingException e) {
            log.error("Failed to send message to kafka.", e);
            throw new UserProfileBusinessException("Failed to send message to kafka to proceed with further validations");
        } catch (UserProfileRepositoryException e) {
            log.error("Failed to update user data with userId {} ", userId, e);
            throw new UserProfileBusinessException("Error while updating user profile.", e);
        }
    }


    @Override
    @CacheEvict(value = ProfileServiceCache.USER_PROFILE_CACHE, key = "#userProfileDTO.userId", condition = "#userProfileDTO != null and #userProfileDTO.userId != null")
    public UserProfileDTO updateAfterValidation(UserProfileDTO userProfileDTO) throws UserProfileBusinessException {
        try {
            UserProfileEO userProfileEO = userProfileMapper.convertDTOTOEO(userProfileDTO);
            log.info("Updating user {} with user details {}", userProfileDTO.getUserId(), userProfileDTO.toString());
            userProfileRepository.update(userProfileDTO.getUserId(), userProfileEO);
            return userProfileMapper.convertEOtoDTO(userProfileEO);
        } catch (UserProfileRepositoryException e) {
            log.info("Failed to update user data {}", userProfileDTO.getUserId());
            throw new UserProfileBusinessException("Error while updating user profile after validation", e);
        }
    }

    @Override
    @CacheEvict(value = ProfileServiceCache.USER_PROFILE_CACHE, key = "#userId")
    public void delete(String userId) {
        try {
            UserProfileDTO userProfileDTO = userProfileMapper.convertEOtoDTO(
                    userProfileRepository.getUserProfileById(userId));
            if (Objects.isNull(userProfileDTO)) {
                throw new NoSuchElementException("No user profile found for user ID: " + userId);
            }
            log.info("Deleting user : {}", userId);
            userProfileRepository.delete(userId);
            log.info("Deleted user : {}", userId);
        } catch (UserProfileRepositoryException e) {
            throw new UserProfileBusinessException("Error while deleting user", e);
        }
    }

    @Override
    public UserProfileValidationResultDTO getStatus(String userId) {
        try {
            Map<String, AttributeValue> result = userProfileRepository.getUserProfileAttributesById(userId, "consolidatedStatus, consolidatedMessage, subscriptionValidations");

            if (result != null && result.containsKey("consolidatedStatus")) {
                UserProfileValidationResultDTO responseDTO = new UserProfileValidationResultDTO();
                responseDTO.setUserId(userId);
                responseDTO.setConsolidatedStatus(result.get("consolidatedStatus").getS());
                if (result.containsKey("consolidatedMessage")) {
                    responseDTO.setConsolidatedMessage(result.get("consolidatedMessage").getS());
                }
                // Check if subscriptionValidations exist and is not empty
                if (result.containsKey("subscriptionValidations") && !result.get("subscriptionValidations").getM().isEmpty()) {
                    Map<String, AttributeValue> subscriptionValidations = result.get("subscriptionValidations").getM();
                    for (Map.Entry<String, AttributeValue> entry : subscriptionValidations.entrySet()) {
                        ProductValidationStatus status = UserProfileMapperHelper.mapToProductValidationStatus(entry.getValue());
                        responseDTO.getSubscriptions().put(entry.getKey(), status);
                    }
                }
                return responseDTO;
            } else {
                log.error("Failed to get status. No user profile found for userId {}", userId);
                throw new UserProfileBusinessException("No user profile found for user ID:" + userId);
            }
        } catch (UserProfileRepositoryException e) {
            log.error("Error while retrieving status for the user with userId {}", userId, e);
            throw new UserProfileBusinessException("Error while retrieving status for the user", e);
        }
    }
    private String generateIdempotencyKey(String email, String pan, String name) throws NoSuchAlgorithmException, NoSuchAlgorithmException {
        String input = email + ":" + pan + ":" + name;
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        byte[] digest = md.digest(input.getBytes());
        return Base64.getEncoder().encodeToString(digest);
    }
    private void sendMessageToKafka(UserProfileDTO userProfileDTO, String eventType) throws KafkaProcessingException {
        try {
            String jsonMessage = JsonUtil.writeToJson(userProfileDTO);
            log.info("Sending message to kafka with user details {}", jsonMessage);
            userProfileSubmissionKafkaProducer.send(jsonMessage, eventType, userProfileDTO.getUserId());
        } catch (KafkaProcessingException e) {
            handleRollback(userProfileDTO);
            log.error("Failed to send message to kafka.", e);
            throw new KafkaProcessingException("Failed to send message to kafka to proceed with further validations", e);
        }
    }

    private void handleRollback(UserProfileDTO userProfileDTO) {
        log.error("Could not send message to kafka for carrying out validations, logging event as not complete in DB");
        UserProfileDTO failedUserProfileDTO = new UserProfileDTO();
        failedUserProfileDTO.setUserId(userProfileDTO.getUserId());
        failedUserProfileDTO.setConsolidatedStatus(ValidationStatusEnum.NOT_COMPLETE.getStatus());
        failedUserProfileDTO.setConsolidatedMessage("Could not perform profile validation due to some unexpected error from a subscribed product.");
        UserProfileEO failedUserProfileEO = userProfileMapper.convertDTOTOEO(failedUserProfileDTO);
        userProfileRepository.update(userProfileDTO.getUserId(), failedUserProfileEO);
        log.error("Rolled back status from IN_PROGRESS to NOT_COMPLETE due to kafka server error");
    }

}

