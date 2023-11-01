package com.crafts.profileservice.repository.impl;

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBScanExpression;
import com.crafts.profileservice.entity.ProductRegistryEO;
import com.crafts.profileservice.exception.UserProfileBusinessException;
import com.crafts.profileservice.repository.ProductRegistryRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class ProductRegistryRepositoryImpl implements ProductRegistryRepository {

    @Autowired
    private DynamoDBMapper dynamoDBMapper;

    @Override
    public List<ProductRegistryEO> findAll() {
        try {
            // Use the DynamoDBMapper to scan the table and map the results to your POJO
            return dynamoDBMapper.scan(ProductRegistryEO.class, new DynamoDBScanExpression());
        } catch (Exception e) {
            throw new UserProfileBusinessException("Failed to load product details", e);
        }
    }

}