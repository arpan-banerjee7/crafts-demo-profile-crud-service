package com.crafts.profileservice.entity;

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBAttribute;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBDocument;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;
@Data
@AllArgsConstructor
@NoArgsConstructor
@DynamoDBDocument
public class ProductValidationStatusEO {
    @DynamoDBAttribute
    private String status;
    @DynamoDBAttribute
    private List<String> errors = new ArrayList<>(); // Initialized to avoid null
}
