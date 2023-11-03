package com.crafts.profileservice.service.impl;

import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.crafts.profileservice.dto.SubscriptionRequestDTO;
import com.crafts.profileservice.dto.TaxIdentifiersDTO;
import com.crafts.profileservice.dto.UserProfileDTO;
import com.crafts.profileservice.dto.UserProfileValidationResultDTO;
import com.crafts.profileservice.entity.UserProfileEO;
import com.crafts.profileservice.enums.ValidationStatusEnum;
import com.crafts.profileservice.exception.KafkaProcessingException;
import com.crafts.profileservice.exception.UserProfileBusinessException;
import com.crafts.profileservice.exception.UserProfileRepositoryException;
import com.crafts.profileservice.mapper.UserProfileMapper;
import com.crafts.profileservice.producer.UserProfileSubmissionKafkaProducer;
import com.crafts.profileservice.repository.impl.UserProfileRepositoryImpl;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@SpringBootTest
public class UserProfileServiceImplTest {

    private UserProfileServiceImpl userProfileService;

    @Mock
    private UserProfileRepositoryImpl userProfileRepository;

    @Mock
    private UserProfileMapper userProfileMapper;

    @Mock
    private UserProfileSubmissionKafkaProducer userProfileSubmissionKafkaProducer;
    private AutoCloseable closeable;

    @BeforeEach
    public void setUp() {
        closeable = MockitoAnnotations.openMocks(this);
        userProfileService = new UserProfileServiceImpl(userProfileRepository, userProfileMapper, userProfileSubmissionKafkaProducer);
    }

    @AfterEach
    public void tearDown() throws Exception {
        closeable.close();
    }

    @Test
    public void testGetUserProfileById_ValidId_ReturnsProfile() {
        String userId = "someUserId";

        UserProfileEO mockEO = new UserProfileEO();
        UserProfileDTO mockDTO = new UserProfileDTO();

        when(userProfileRepository.getUserProfileById(userId)).thenReturn(mockEO);
        when(userProfileMapper.convertEOtoDTO(mockEO)).thenReturn(mockDTO);

        UserProfileDTO result = userProfileService.getUserProfileById(userId);

        assertEquals(mockDTO, result);
    }

    @Test
    public void testGetUserProfileById_InvalidId_ThrowsException() {
        String userId = "someUserId";

        when(userProfileRepository.getUserProfileById(userId)).thenReturn(null);
        assertThrows(NoSuchElementException.class, () -> {
            userProfileService.getUserProfileById(userId);
        });

    }

    @Test
    public void testSaveUserProfile_ValidProfile_SavesSuccessfully() throws KafkaProcessingException {
        UserProfileDTO mockInputDTO = new UserProfileDTO();
        mockInputDTO.setEmail("test@123");
        mockInputDTO.setLegalName("test-admin");
        mockInputDTO.setSubscriptions(List.of("product_1"));
        mockInputDTO.setTaxIdentifiers(new TaxIdentifiersDTO("pan", "ein"));
        UserProfileEO mockEO = new UserProfileEO();
        UserProfileDTO mockOutputDTO = new UserProfileDTO();

        when(userProfileMapper.convertDTOTOEO(mockInputDTO)).thenReturn(mockEO);
        when(userProfileRepository.save(mockEO)).thenReturn(mockEO);
        when(userProfileMapper.convertEOtoDTO(mockEO)).thenReturn(mockOutputDTO);

        UserProfileDTO result = userProfileService.saveUserProfile(mockInputDTO);

        assertEquals(mockOutputDTO, result);
        verify(userProfileSubmissionKafkaProducer, times(1)).send(any(), eq("USER_PROFILE_CREATE"), any());
    }

    @Test
    public void testSaveUserProfile_EmptySubscriptions_ThrowsException() {
        UserProfileDTO mockInputDTO = new UserProfileDTO();
        mockInputDTO.setSubscriptions(Collections.emptyList());
        assertThrows(IllegalArgumentException.class, () -> {
            userProfileService.saveUserProfile(mockInputDTO);
        });
    }

    @Test
    public void testAddSubscription_ValidInput_AddsSubscription() throws KafkaProcessingException {
        String userId = "product_1";
        SubscriptionRequestDTO requestDTO = new SubscriptionRequestDTO();
        requestDTO.setProductId("product_1");

        UserProfileEO mockEO = new UserProfileEO();
        mockEO.setConsolidatedStatus(ValidationStatusEnum.IN_PROGRESS.getStatus());
        mockEO.setSubscriptions(new ArrayList<>());

        UserProfileDTO mockDTO = new UserProfileDTO();
        mockDTO.setConsolidatedStatus(ValidationStatusEnum.IN_PROGRESS.getStatus());
        mockDTO.setSubscriptions(new ArrayList<>());

        // Mocking the Repository behavior
        when(userProfileRepository.getUserProfileById(userId)).thenReturn(mockEO);

        // Mocking the Mapper behavior
        when(userProfileMapper.convertEOtoDTO(mockEO)).thenReturn(mockDTO);
        when(userProfileMapper.convertDTOTOEO(mockDTO)).thenReturn(mockEO);

        userProfileService.addSubscription(userId, requestDTO);

        assertTrue(mockDTO.getSubscriptions().contains(requestDTO.getProductId()));
        verify(userProfileSubmissionKafkaProducer, times(1)).send(any(), eq("USER_PROFILE_ADD_SUBSCRIPTION"), any());
    }

    @Test
    public void testUpdate_Success() throws UserProfileBusinessException, KafkaProcessingException {
        String userId = "user123";
        UserProfileDTO mockDTO = new UserProfileDTO();
        UserProfileEO mockEO = new UserProfileEO();

        // Mocking behavior
        when(userProfileMapper.convertDTOTOEO(any())).thenReturn(mockEO);
        userProfileRepository.update(anyString(), any());
        doNothing().when(userProfileSubmissionKafkaProducer).send(any(), eq("USER_PROFILE_UPDATE"), any());

        // Call the method
        userProfileService.update(userId, mockDTO);

        verify(userProfileRepository, times(1)).update(eq(userId), any());
        verify(userProfileSubmissionKafkaProducer, times(1)).send(any(), eq("USER_PROFILE_UPDATE"), any());
    }

    // 2. KafkaProcessingException scenario
    @Test
    public void testUpdate_KafkaException() throws KafkaProcessingException {
        String userId = "user123";
        UserProfileDTO mockDTO = new UserProfileDTO();

        when(userProfileMapper.convertDTOTOEO(any())).thenReturn(new UserProfileEO());
        doThrow(KafkaProcessingException.class).when(userProfileSubmissionKafkaProducer).send(any(), eq("USER_PROFILE_UPDATE"), any());
        assertThrows(UserProfileBusinessException.class, () -> {
            userProfileService.update(userId, mockDTO);
        });
    }

    // 3. UserProfileRepositoryException scenario
    @Test
    public void testUpdate_Exception() throws UserProfileBusinessException {
        String userId = "user123";
        UserProfileDTO mockDTO = new UserProfileDTO();

        when(userProfileMapper.convertDTOTOEO(any())).thenReturn(new UserProfileEO());
        doThrow(UserProfileRepositoryException.class).when(userProfileRepository).update(anyString(), any());
        assertThrows(UserProfileBusinessException.class, () -> {
            userProfileService.update(userId, mockDTO);
        });
    }

    @Test
    public void testUpdateAfterValidation_Success() throws UserProfileBusinessException {
        UserProfileDTO mockDTO = new UserProfileDTO();
        mockDTO.setUserId("user123");
        UserProfileEO mockEO = new UserProfileEO();

        when(userProfileMapper.convertDTOTOEO(mockDTO)).thenReturn(mockEO);
        when(userProfileRepository.update(anyString(), any())).thenReturn(mockEO);
        when(userProfileMapper.convertEOtoDTO(mockEO)).thenReturn(mockDTO);

        UserProfileDTO result = userProfileService.updateAfterValidation(mockDTO);
        assertEquals(mockDTO, result);
    }

    @Test
    public void testUpdateAfterValidation_Exception() {
        UserProfileDTO mockDTO = new UserProfileDTO();
        mockDTO.setUserId("user123");

        when(userProfileMapper.convertDTOTOEO(mockDTO)).thenReturn(new UserProfileEO());
        when(userProfileRepository.update(anyString(), any())).thenThrow(UserProfileRepositoryException.class);
        assertThrows(UserProfileBusinessException.class, () -> {
            userProfileService.updateAfterValidation(mockDTO);
        });
    }

    @Test
    public void testDelete_Success() {
        String userId = "user123";
        UserProfileEO mockEO = new UserProfileEO();
        UserProfileDTO mockDTO = new UserProfileDTO();

        when(userProfileRepository.getUserProfileById(userId)).thenReturn(mockEO);
        when(userProfileMapper.convertEOtoDTO(mockEO)).thenReturn(mockDTO);
        doNothing().when(userProfileRepository).delete(userId);

        userProfileService.delete(userId);
    }

    @Test
    public void testDelete_UserNotFound() {
        String userId = "user123";
        when(userProfileRepository.getUserProfileById(userId)).thenReturn(null);
        assertThrows(NoSuchElementException.class, () -> {
            userProfileService.delete(userId);
        });
    }

    @Test
    public void testDelete_Exception() {
        String userId = "user123";
        UserProfileEO mockEO = new UserProfileEO();
        when(userProfileRepository.getUserProfileById(userId)).thenReturn(mockEO);
        doThrow(NoSuchElementException.class).when(userProfileRepository).delete(userId);
        assertThrows(NoSuchElementException.class, () -> {
            userProfileService.delete(userId);
        });
    }

    @Test
    public void testGetStatus_Success() {
        String userId = "user123";
        Map<String, AttributeValue> mockResult = new HashMap<>();
        mockResult.put("consolidatedStatus", new AttributeValue().withS("SampleStatus"));
        mockResult.put("consolidatedMessage", new AttributeValue().withS("SampleMessage"));

        when(userProfileRepository.getUserProfileAttributesById(userId, "consolidatedStatus, consolidatedMessage, subscriptionValidations")).thenReturn(mockResult);

        UserProfileValidationResultDTO resultDTO = userProfileService.getStatus(userId);
        assertEquals("SampleStatus", resultDTO.getConsolidatedStatus());
    }

    @Test
    public void testGetStatus_WithSubscriptionValidations() {
        String userId = "user123";
        Map<String, AttributeValue> mockResult = new HashMap<>();
        mockResult.put("consolidatedStatus", new AttributeValue().withS("SampleStatus"));
        mockResult.put("consolidatedMessage", new AttributeValue().withS("SampleMessage"));
        Map<String, AttributeValue> subscriptions = new HashMap<>();
        subscriptions.put("product1", new AttributeValue().withS("Validated"));
        mockResult.put("subscriptionValidations", new AttributeValue().withM(subscriptions));

        when(userProfileRepository.getUserProfileAttributesById(userId, "consolidatedStatus, consolidatedMessage, subscriptionValidations")).thenReturn(mockResult);

        UserProfileValidationResultDTO resultDTO = userProfileService.getStatus(userId);
        assertEquals("SampleStatus", resultDTO.getConsolidatedStatus());
        assertEquals(1, resultDTO.getSubscriptions().size());
        assertTrue(resultDTO.getSubscriptions().containsKey("product1"));
    }

    @Test
    public void testGetStatus_UserNotFound() {
        String userId = "user123";
        when(userProfileRepository.getUserProfileAttributesById(userId, "consolidatedStatus, subscriptionValidations")).thenReturn(null);

        assertThrows(UserProfileBusinessException.class, () -> userProfileService.getStatus(userId));
    }

    @Test
    public void testGetStatus_RepositoryException() {
        String userId = "user123";
        when(userProfileRepository.getUserProfileAttributesById(userId, "consolidatedStatus, subscriptionValidations")).thenThrow(UserProfileRepositoryException.class);

        assertThrows(UserProfileBusinessException.class, () -> userProfileService.getStatus(userId));
    }

}


