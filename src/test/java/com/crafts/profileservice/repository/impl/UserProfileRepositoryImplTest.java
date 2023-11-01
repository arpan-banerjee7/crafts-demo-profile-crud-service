package com.crafts.profileservice.repository.impl;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapperConfig;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBSaveExpression;
import com.amazonaws.services.dynamodbv2.model.*;
import com.crafts.profileservice.entity.UserProfileEO;
import com.crafts.profileservice.exception.UserProfileRepositoryException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.NoSuchElementException;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

public class UserProfileRepositoryImplTest {

    @Mock
    private DynamoDBMapper dynamoDBMapper;

    @Mock
    private AmazonDynamoDB dynamoDBClient;

    private UserProfileRepositoryImpl userProfileRepository;
    private AutoCloseable closeable;

    @BeforeEach
    public void setUp() {
        closeable = MockitoAnnotations.openMocks(this);
        userProfileRepository = new UserProfileRepositoryImpl(dynamoDBMapper, dynamoDBClient);
    }

    @AfterEach
    public void tearDown() throws Exception {
        closeable.close();
    }

    @Test
    public void testGetUserProfileById() {
        when(dynamoDBMapper.load(UserProfileEO.class, "testId")).thenReturn(new UserProfileEO());
        userProfileRepository.getUserProfileById("testId");

        doThrow(new RuntimeException()).when(dynamoDBMapper).load(UserProfileEO.class, "testId");
        assertThrows(UserProfileRepositoryException.class, () -> userProfileRepository.getUserProfileById("testId"));
    }

    @Test
    public void testGetUserProfileAttributesById() {
        when(dynamoDBClient.getItem(any(GetItemRequest.class))).thenReturn(new GetItemResult());
        userProfileRepository.getUserProfileAttributesById("testId", "projectionExpression");

        doThrow(new ResourceNotFoundException("Table not found")).when(dynamoDBClient).getItem(any(GetItemRequest.class));
        assertThrows(NoSuchElementException.class, () -> userProfileRepository.getUserProfileAttributesById("testId", "projectionExpression"));

        doThrow(new AmazonDynamoDBException("DynamoDB error")).when(dynamoDBClient).getItem(any(GetItemRequest.class));
        assertThrows(UserProfileRepositoryException.class, () -> userProfileRepository.getUserProfileAttributesById("testId", "projectionExpression"));
    }

    @Test
    public void testSaveUserProfile() {
        // No need to stub the dynamoDBMapper.save method when it's a successful save.
        userProfileRepository.save(new UserProfileEO());

        doThrow(new ConditionalCheckFailedException("User exists")).when(dynamoDBMapper).save(any(UserProfileEO.class));
        assertThrows(UserProfileRepositoryException.class, () -> userProfileRepository.save(new UserProfileEO()));
    }

    @Test
    public void testUpdateUserProfile() {
        // No need to stub the dynamoDBMapper.save method when it's a successful update.
        userProfileRepository.update("testId", new UserProfileEO());

        doThrow(new ConditionalCheckFailedException("Condition failed"))
                .when(dynamoDBMapper)
                .save(any(UserProfileEO.class), any(DynamoDBSaveExpression.class), any(DynamoDBMapperConfig.class));
        assertThrows(NoSuchElementException.class, () -> userProfileRepository.update("testId", new UserProfileEO()));
    }


    @Test
    public void testDeleteUserProfile() {
        when(dynamoDBMapper.load(UserProfileEO.class, "testId")).thenReturn(new UserProfileEO());
        userProfileRepository.delete("testId");

        when(dynamoDBMapper.load(UserProfileEO.class, "testId")).thenReturn(null);
        assertThrows(NoSuchElementException.class, () -> userProfileRepository.delete("testId"));
    }
}
