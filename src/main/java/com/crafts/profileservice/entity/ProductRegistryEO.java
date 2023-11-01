package com.crafts.profileservice.entity;

import com.amazonaws.services.dynamodbv2.datamodeling.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@DynamoDBTable(tableName = "product_registry")
public class ProductRegistryEO {

    @DynamoDBHashKey
    private String productId;

    @DynamoDBAttribute
    private String productValidationUrl;
}
