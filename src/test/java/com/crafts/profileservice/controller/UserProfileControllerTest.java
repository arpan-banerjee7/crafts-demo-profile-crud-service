package com.crafts.profileservice.controller;

import com.crafts.profileservice.advice.CustomControllerAdvice;
import com.crafts.profileservice.dto.SubscriptionRequestDTO;
import com.crafts.profileservice.dto.UserProfileDTO;
import com.crafts.profileservice.exception.UserProfileBusinessException;
import com.crafts.profileservice.service.UserProfileService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.NoSuchElementException;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class UserProfileControllerTest {

    private MockMvc mockMvc;
    private AutoCloseable closeable;
    @Mock
    private UserProfileService userProfileService;

    @BeforeEach
    public void setUp() {
        closeable = MockitoAnnotations.openMocks(this);
        UserProfileController userProfileController = new UserProfileController(userProfileService);
        this.mockMvc = MockMvcBuilders.standaloneSetup(userProfileController)
                .setControllerAdvice(new CustomControllerAdvice())
                .build();
    }

    @AfterEach
    public void tearDown() throws Exception {
        closeable.close();
    }

    @Test
    public void testGetUserProfile() throws Exception {
        UserProfileDTO mockProfile = new UserProfileDTO();
        when(userProfileService.getUserProfileById("1")).thenReturn(mockProfile);

        mockMvc.perform(get("/user/1"))
                .andExpect(status().isOk());

        when(userProfileService.getUserProfileById("2")).thenThrow(new NoSuchElementException("User not found"));
        mockMvc.perform(get("/user/2"))
                .andExpect(status().isNotFound());
    }

    @Test
    public void testCreateUserProfile() throws Exception {
        UserProfileDTO mockProfile = new UserProfileDTO();
        when(userProfileService.saveUserProfile(any())).thenReturn(mockProfile);

        // Convert the UserProfileDTO object to a JSON string
        ObjectMapper objectMapper = new ObjectMapper();
        String mockProfileJson = objectMapper.writeValueAsString(mockProfile);

        mockMvc.perform(post("/user/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mockProfileJson))
                .andExpect(status().isCreated());
    }

    @Test
    public void testUpdateUserProfile() throws Exception {
        UserProfileDTO mockProfile = new UserProfileDTO();
        ObjectMapper objectMapper = new ObjectMapper();
        String mockProfileJson = objectMapper.writeValueAsString(mockProfile);

        // Test case when the user profile update is successful
        doNothing().when(userProfileService).update(anyString(), any());
        mockMvc.perform(put("/user/update/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mockProfileJson))
                .andExpect(status().isNoContent());

        // Test case when the user profile is not found
        doThrow(new NoSuchElementException("User not found")).when(userProfileService).update(eq("2"), any());
        mockMvc.perform(put("/user/update/2")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mockProfileJson))
                .andExpect(status().isNotFound());
    }

    @Test
    public void testAddSubscription_Successful() throws Exception {
        SubscriptionRequestDTO mockSubscription = new SubscriptionRequestDTO();
        ObjectMapper objectMapper = new ObjectMapper();
        String mockSubscriptionJson = objectMapper.writeValueAsString(mockSubscription);

        doNothing().when(userProfileService).addSubscription(anyString(), any());
        mockMvc.perform(put("/user/{userId}/subscriptions", "1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mockSubscriptionJson))
                .andExpect(status().isOk());
    }

    @Test
    public void testAddSubscription_UserProfileBusinessException() throws Exception {
        SubscriptionRequestDTO mockSubscription = new SubscriptionRequestDTO();
        ObjectMapper objectMapper = new ObjectMapper();
        String mockSubscriptionJson = objectMapper.writeValueAsString(mockSubscription);

        doThrow(new UserProfileBusinessException("Business Error")).when(userProfileService).addSubscription(anyString(), any());
        mockMvc.perform(put("/user/{userId}/subscriptions", "1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mockSubscriptionJson))
                .andExpect(status().isInternalServerError())
                .andExpect(content().string("Business Error"));
    }

    @Test
    public void testDeleteUserProfile() throws Exception {
        doNothing().when(userProfileService).delete(anyString());
        mockMvc.perform(delete("/user/delete/1"))
                .andExpect(status().isOk());

        doThrow(new NoSuchElementException("User not found")).when(userProfileService).delete("2");
        mockMvc.perform(delete("/user/delete/2"))
                .andExpect(status().isNotFound());
    }

    @Test
    public void testGetStatus() throws Exception {
        UserProfileValidationResultDTO mockResult = new UserProfileValidationResultDTO();
        when(userProfileService.getStatus("1")).thenReturn(mockResult);

        mockMvc.perform(get("/user/status/1"))
                .andExpect(status().isOk());

        when(userProfileService.getStatus("2")).thenThrow(new NoSuchElementException("User not found"));
        mockMvc.perform(get("/user/status/2"))
                .andExpect(status().isNotFound());
    }
}
