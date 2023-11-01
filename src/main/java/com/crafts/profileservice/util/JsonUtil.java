package com.crafts.profileservice.util;

import com.crafts.profileservice.exception.JsonDeserializationException;
import com.crafts.profileservice.exception.JsonSerializationException;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public final class JsonUtil {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    public static String writeToJson(Object content) {
        try {
            OBJECT_MAPPER.setSerializationInclusion(JsonInclude.Include.NON_NULL);
            return OBJECT_MAPPER.writeValueAsString(content);
        } catch (JsonProcessingException e) {
            log.error("JSON serialization error: {}", e.getMessage());
            throw new JsonSerializationException("JSON serialization error", e);
        }
    }

    public static <T> T readValue(String content, Class<T> valueType) {
        try {
            return OBJECT_MAPPER.readValue(content, valueType);
        } catch (JsonProcessingException e) {
            log.error("JSON deserialization error: {}", e.getMessage());
            throw new JsonDeserializationException("JSON deserialization error", e);
        }
    }
}
