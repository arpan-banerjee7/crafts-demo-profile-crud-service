package com.crafts.profileservice.repository;

import com.crafts.profileservice.entity.UserProfileEO;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;

import java.util.Map;

public interface UserProfileRepository {
    UserProfileEO save(UserProfileEO userProfile);

    UserProfileEO getUserProfileById(String userId);

    void delete(String userId);

    UserProfileEO update(String userId, UserProfileEO userProfile);

    Map<String,AttributeValue> getUserProfileAttributesById(String userId, String projectExpression);
}
