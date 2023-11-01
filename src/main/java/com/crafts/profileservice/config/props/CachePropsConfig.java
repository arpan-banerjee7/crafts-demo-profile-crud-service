package com.crafts.profileservice.config.props;

import lombok.Getter;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.stereotype.Component;

@Component
@RefreshScope
@Getter
@Setter
public class CachePropsConfig {

    @Value("${cache.lookup.timeout}")
    private int operationTimeout;

    @Value("${cache.memcache.servers}")
    private String memcacheServers;

    @Value("${cache.timeout}")
    private int expiration;

    @Value("${cache.exception.threshold.timeout}")
    private int timoutExceptionThreshold;

    @Value("${cache.mute.exception}")
    private boolean muteException;

    @Value("${cache.key.separator}")
    private String cacheKeySeparator;
}
