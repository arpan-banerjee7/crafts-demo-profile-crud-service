package com.crafts.profileservice.entity;

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBAttribute;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBDocument;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@DynamoDBDocument
public class TaxIdentifiersEO {

    @DynamoDBAttribute
    private String pan;

    @DynamoDBAttribute
    private String ein;

}
