package com.crafts.profileservice.config.secrets;

import lombok.Getter;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.stereotype.Component;

@Component
@Getter
@Setter
public class DatabaseSecretConfig {
    @Value("${aws.dynamoDB.service-endpoint}")
    private String awsDynamoDbServiceEndpoint;

    @Value("${aws.dynamoDB.signing-region}")
    private String awsDynamoDbServiceSigningRegion;

    @Value("${aws.dynamoDB.consumer.access-key}")
    private String awsDynamoDbServiceAccesKey;

    @Value("${aws.dynamoDB.consumer.secret-key}")
    private String awsDynamoDbServiceSecretKey;
}
