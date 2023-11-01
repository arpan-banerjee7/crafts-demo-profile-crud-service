package com.crafts.profileservice.service;


import com.crafts.profileservice.dto.UserProfileDTO;
import com.crafts.profileservice.dto.UserProfileValidationResponseDTO;
import com.crafts.profileservice.exception.UserProfileBusinessException;

public interface UserProfileValidationService {
    void validateUserProfile(String message) throws UserProfileBusinessException;
    void validateUserProfile(UserProfileDTO userProfileDTO) throws UserProfileBusinessException;
}
