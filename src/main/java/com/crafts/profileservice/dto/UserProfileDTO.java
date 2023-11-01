package com.crafts.profileservice.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Data
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class UserProfileDTO implements Serializable {

    private String userId;

    private String productId;

    private boolean createFlow;

    private String companyName;

    private String legalName;

    private AddressDTO businessAddress;

    private AddressDTO legalAddress;

    private TaxIdentifiersDTO taxIdentifiers;

    private String email;

    private String website;

    private String consolidatedStatus;

    private String consolidatedMessage;

    private String timestamp;

    private List<String> subscriptions;

    private List<String> existingSubscriptions;

    private Map<String, ProductValidationStatus> subscriptionValidations = new HashMap<>();

}


