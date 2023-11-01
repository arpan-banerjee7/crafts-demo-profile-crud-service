package com.crafts.profileservice.mapper;

import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.crafts.profileservice.dto.ProductValidationStatus;

import java.util.Map;
import java.util.stream.Collectors;

public class UserProfileMapperHelper {
    public static ProductValidationStatus mapToProductValidationStatus(AttributeValue value) {
        if (value == null || value.getM() == null) {
            return null;
        }

        Map<String, AttributeValue> valueMap = value.getM();
        ProductValidationStatus status = new ProductValidationStatus();

        if (valueMap.containsKey("status")) {
            status.setStatus(valueMap.get("status").getS());
        }

        if (valueMap.containsKey("errors")) {
            status.setErrors(valueMap.get("errors").getL().stream()
                    .map(AttributeValue::getS)
                    .collect(Collectors.toList()));
        }

        return status;
    }

}
