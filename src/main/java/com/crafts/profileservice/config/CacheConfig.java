package com.crafts.profileservice.config;

import com.crafts.profileservice.config.props.CachePropsConfig;
import com.crafts.profileservice.constans.ServiceConstants;
import com.google.code.ssm.CacheFactory;
import com.google.code.ssm.api.format.SerializationType;
import com.google.code.ssm.config.DefaultAddressProvider;
import com.google.code.ssm.providers.spymemcached.MemcacheClientFactoryImpl;
import com.google.code.ssm.providers.spymemcached.SpymemcachedConfiguration;
import com.google.code.ssm.spring.ExtendedSSMCacheManager;
import com.google.code.ssm.spring.SSMCache;
import com.google.code.ssm.spring.SSMCacheManager;
import lombok.extern.slf4j.Slf4j;
import net.spy.memcached.MemcachedClient;
import net.spy.memcached.spring.MemcachedClientFactoryBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cache.CacheManager;
import org.springframework.cache.transaction.TransactionAwareCacheManagerProxy;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.DependsOn;

import java.util.Collections;

import static com.crafts.profileservice.constans.ProfileServiceCache.USER_PROFILE_CACHE;

@Configuration
@EnableCaching
@Slf4j
public class CacheConfig {

    @Autowired
    private CachePropsConfig cachePropsConfig;

    @Bean("profileServiceCacheFactory")
    public CacheFactory getCacheFactory() {
        CacheFactory cacheFactory = new CacheFactory();
        cacheFactory.setCacheClientFactory(new MemcacheClientFactoryImpl());
        cacheFactory.setAddressProvider(new DefaultAddressProvider(cachePropsConfig.getMemcacheServers()));
        SpymemcachedConfiguration cacheConfiguration = new SpymemcachedConfiguration();
        cacheConfiguration.setConsistentHashing(ServiceConstants.TRUE);
        cacheConfiguration.setUseBinaryProtocol(ServiceConstants.FALSE);
        cacheConfiguration.setOperationTimeout(cachePropsConfig.getOperationTimeout());
        cacheConfiguration.setTimeoutExceptionThreshold(cachePropsConfig.getTimoutExceptionThreshold());
        cacheConfiguration.setUseNameAsKeyPrefix(ServiceConstants.TRUE);
        log.error("****************************** Cache Key prefix saparator = {}", cachePropsConfig.getCacheKeySeparator());
        cacheConfiguration.setKeyPrefixSeparator(cachePropsConfig.getCacheKeySeparator());
        cacheFactory.setDefaultSerializationType(SerializationType.JAVA);
        cacheFactory.setCacheName(ServiceConstants.PROFILE_SERVICE_CACHE);
        cacheFactory.setCacheAliases(Collections.singleton(USER_PROFILE_CACHE));
        cacheFactory.setConfiguration(cacheConfiguration);
        return cacheFactory;
    }

    @Bean("cacheManager")
    @DependsOn("profileServiceCacheFactory")
    @RefreshScope
    public CacheManager getCacheManager(@Qualifier("profileServiceCacheFactory") CacheFactory profileServiceCacheFactory)
            throws Exception {
        SSMCacheManager ssmCacheManager = new ExtendedSSMCacheManager();
        SSMCache ssmCache = new SSMCache(profileServiceCacheFactory.getObject(), cachePropsConfig.getExpiration(), ServiceConstants.FALSE, ServiceConstants.TRUE,
                cachePropsConfig.isMuteException());
        ssmCacheManager.setCaches(Collections.singleton(ssmCache));
        ssmCacheManager.afterPropertiesSet();
        return new TransactionAwareCacheManagerProxy(ssmCacheManager);
    }

    @Bean
    public MemcachedClient memcachedClient(@Autowired MemcachedClientFactoryBean memcachedClientFactoryBean) throws Exception {
        return (MemcachedClient) memcachedClientFactoryBean.getObject();
    }

    @Bean
    public MemcachedClientFactoryBean memcachedClientFactoryBean() {
        MemcachedClientFactoryBean clientFactoryBean = new MemcachedClientFactoryBean();
        clientFactoryBean.setServers(cachePropsConfig.getMemcacheServers());
        clientFactoryBean.setOpTimeout(cachePropsConfig.getOperationTimeout());
        clientFactoryBean.setTimeoutExceptionThreshold(cachePropsConfig.getTimoutExceptionThreshold());
        return clientFactoryBean;
    }

}
