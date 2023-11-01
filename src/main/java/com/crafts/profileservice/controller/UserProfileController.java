package com.crafts.profileservice.controller;

import com.crafts.profileservice.dto.SubscriptionRequestDTO;
import com.crafts.profileservice.dto.UserProfileDTO;
import com.crafts.profileservice.dto.UserProfileValidationResultDTO;
import com.crafts.profileservice.service.UserProfileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;

@RestController
@RequestMapping("/user")
@CrossOrigin
public class UserProfileController {

    private final UserProfileService userProfileService;

    public UserProfileController(UserProfileService userProfileService) {
        this.userProfileService = userProfileService;
    }

    @Operation(summary = "Get user profile by ID")
    @GetMapping("/{userId}")
    public ResponseEntity<UserProfileDTO> getUserProfile(
            @Parameter(description = "The user ID", required = true)
            @PathVariable("userId") String userId) {
        UserProfileDTO userProfile = userProfileService.getUserProfileById(userId);
        return ResponseEntity.ok(userProfile);
    }

    @Operation(summary = "Create user profile")
    @ApiResponse(responseCode = "201", description = "User profile created")
    @PostMapping("/create")
    public ResponseEntity<UserProfileDTO> createUserProfile(
            @RequestBody UserProfileDTO userProfile) {
        UserProfileDTO userProfileDTO = userProfileService.saveUserProfile(userProfile);
        URI locationUri = UriComponentsBuilder
                .fromPath("/users/{userId}")
                .buildAndExpand(userProfileDTO)
                .toUri();
        return ResponseEntity.created(locationUri).body(userProfileDTO);
    }

    @Operation(summary = "Update user profile")
    @PutMapping("/update/{userId}")
    public ResponseEntity<Void> updateUserProfile(
            @Parameter(description = "The user ID", required = true)
            @PathVariable("userId") String userId,
            @RequestBody UserProfileDTO userProfile) {
        userProfileService.update(userId, userProfile);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Get user status")
    @GetMapping("/status/{userId}")
    public ResponseEntity<UserProfileValidationResultDTO> getStatus(@PathVariable("userId") String userId) {
        UserProfileValidationResultDTO userProfileValidationResultDTO = userProfileService.getStatus(userId);
        return ResponseEntity.ok(userProfileValidationResultDTO);
    }

    @Operation(summary = "Subscribe to a new product")
    @PutMapping("/{userId}/subscriptions")
    public ResponseEntity<Void> addSubscription(@PathVariable("userId") String userId,
                                                @RequestBody SubscriptionRequestDTO subscriptionRequestDTO) {
        userProfileService.addSubscription(userId, subscriptionRequestDTO);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "Delete user profile")
    @ApiResponse(responseCode = "200", description = "User profile deleted")
    @DeleteMapping("/delete/{userId}")
    public ResponseEntity<Void> deleteUserProfile(
            @Parameter(description = "The user ID", required = true)
            @PathVariable("userId") String userId) {
        userProfileService.delete(userId);
        return ResponseEntity.ok().build();
    }

}
