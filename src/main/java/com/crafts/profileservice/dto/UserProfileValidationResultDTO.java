package com.crafts.profileservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.HashMap;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserProfileValidationResultDTO {
    private String userId;
    private String consolidatedStatus;
    private String consolidatedMessage;
    private Map<String, ProductValidationStatus> subscriptions = new HashMap<>(); // Initialized to avoid null

}
