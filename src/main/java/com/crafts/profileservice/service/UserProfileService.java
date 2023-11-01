package com.crafts.profileservice.service;

import com.crafts.profileservice.dto.SubscriptionRequestDTO;
import com.crafts.profileservice.dto.UserProfileDTO;
import com.crafts.profileservice.dto.UserProfileValidationResultDTO;

public interface UserProfileService {

    UserProfileValidationResultDTO getStatus(String userId);

    UserProfileDTO saveUserProfile(UserProfileDTO userProfile);

    UserProfileDTO getUserProfileById(String userId);

    void delete(String userId);

    UserProfileDTO updateAfterValidation(UserProfileDTO userProfile);
  
    void update(String userId, UserProfileDTO userProfile);

    void addSubscription(String userId, SubscriptionRequestDTO subscriptionRequestDTO);
}
