package com.crafts.profileservice.mapper;

import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.crafts.profileservice.dto.ProductValidationStatus;
import com.crafts.profileservice.dto.UserProfileDTO;
import com.crafts.profileservice.entity.UserProfileEO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.stream.Collectors;

@Mapper(componentModel = "spring")
@Component
public interface UserProfileMapper {
    @Mapping(target = "productId", ignore = true)
    @Mapping(target = "existingSubscriptions", ignore = true)
    UserProfileDTO convertEOtoDTO(UserProfileEO userProfile);

    @Mapping(target = "consolidatedStatus", expression = "java(userProfile.getConsolidatedStatus() != null ? userProfile.getConsolidatedStatus() : String.valueOf(com.crafts.profileservice.enums.ValidationStatusEnum.IN_PROGRESS))")
    @Mapping(target = "timestamp", expression = "java(java.time.Instant.now().toString())")
    UserProfileEO convertDTOTOEO(UserProfileDTO userProfile);

}
