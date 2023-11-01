package com.crafts.profileservice.controller;

import com.crafts.profileservice.dto.UserProfileDTO;
import com.crafts.profileservice.service.UserProfileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.NoSuchElementException;

@RestController
@RequestMapping(value = "/cache")
public class CacheController {

    @Autowired
    private CacheManager cacheManager;

    @Autowired
    private UserProfileService userProfileService;

    @Operation(summary = "Get cached data with a specific key")
    @GetMapping(value = "/{cacheName}/{key}", produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_JSON_VALUE})
    public ResponseEntity<UserProfileDTO> getCacheWithKey(
            @Parameter(description = "The name of the cache", required = true)
            @PathVariable("cacheName") String cacheName,
            @Parameter(description = "The key for the cached data", required = true)
            @PathVariable("key") String key) throws NoSuchElementException {
        Cache.ValueWrapper value = getCache(cacheName).get(key);
        if (value != null) {
            return ResponseEntity.status(HttpStatus.OK).body((UserProfileDTO) value.get());
        }
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }

    @Operation(summary = "Evict cached data with a specific key")
    @DeleteMapping(value = "/{cacheName}/{key}", produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_JSON_VALUE})
    public ResponseEntity<Void> evict(
            @Parameter(description = "The name of the cache", required = true)
            @PathVariable("cacheName") String cacheName,
            @Parameter(description = "The key for the cached data", required = true)
            @PathVariable("key") String key) throws NoSuchElementException {
        getCache(cacheName).evict(key);
        return ResponseEntity.ok().build();
    }

    private Cache getCache(String cacheName) throws NoSuchElementException {
        Cache cache = cacheManager.getCache(cacheName);
        if (cache == null)
            throw new NoSuchElementException("Cache with Name " + cacheName + " does not exists.");
        return cache;
    }
}
