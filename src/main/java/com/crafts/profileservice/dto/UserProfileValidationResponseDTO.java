package com.crafts.profileservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserProfileValidationResponseDTO {
    String userId;
    String productId;
    String message;
    List<String> errors;
}
