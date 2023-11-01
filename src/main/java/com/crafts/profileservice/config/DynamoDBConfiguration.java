package com.crafts.profileservice.config;

import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper;
import com.crafts.profileservice.config.secrets.DatabaseSecretConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DynamoDBConfiguration {

    @Bean
    public DynamoDBMapper dynamoDBMapper() {
        return new DynamoDBMapper(buildAmazonDynamoDB());
    }

    @Autowired
    private DatabaseSecretConfig databaseSecretConfig;

    @Bean
    public AmazonDynamoDB buildAmazonDynamoDB() {
        return AmazonDynamoDBClientBuilder.standard()
                .withEndpointConfiguration(new AwsClientBuilder.EndpointConfiguration(
                        databaseSecretConfig.getAwsDynamoDbServiceEndpoint(),
                        databaseSecretConfig.getAwsDynamoDbServiceSigningRegion()))
                .withCredentials(new AWSStaticCredentialsProvider(new BasicAWSCredentials(
                        databaseSecretConfig.getAwsDynamoDbServiceAccesKey(),
                        databaseSecretConfig.getAwsDynamoDbServiceSecretKey()))).build();
    }
}
