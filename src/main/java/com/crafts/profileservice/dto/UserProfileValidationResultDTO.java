package com.crafts.profileservice.dto;

import lombok.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
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
