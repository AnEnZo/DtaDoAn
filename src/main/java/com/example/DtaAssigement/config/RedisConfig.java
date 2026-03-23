package com.example.DtaAssigement.config;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.impl.LaissezFaireSubTypeValidator;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * Redis Cache Configuration
 * Configures both Spring Cache and RedisTemplate for custom CacheManager
 * Handles Redis connection failures gracefully - app will continue without
 * caching if Redis is unavailable
 */
@Slf4j
@Configuration
@EnableCaching
public class RedisConfig {

        @Autowired(required = false)
        private RedisConnectionFactory redisConnectionFactory;

        /**
         * Check Redis connection on startup
         * If Redis is unavailable, log warning but don't fail application startup
         */
        @PostConstruct
        public void checkRedisConnection() {
                if (redisConnectionFactory == null) {
                        log.warn("⚠️ Redis ConnectionFactory is not available. Redis caching will be disabled.");
                        return;
                }

                try {
                        // Test the connection
                        redisConnectionFactory.getConnection().ping();
                        log.info("✅ Redis connection established successfully");
                } catch (Exception e) {
                        log.warn("⚠️ Failed to connect to Redis: {}. Application will continue without caching functionality.",
                                        e.getMessage());
                        log.debug("Redis connection error details:", e);
                }
        }

        /**
         * RedisTemplate bean for custom CacheManager
         * Used by com.example.DtaAssigement.aop.caching.CacheManager
         * Returns null if Redis connection fails to prevent app crash
         */
        @Bean
        public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
                try {
                        RedisTemplate<String, Object> template = new RedisTemplate<>();
                        template.setConnectionFactory(connectionFactory);

                        // JSON serialization with type information
                        ObjectMapper mapper = new ObjectMapper();
                        mapper.setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.ANY);
                        mapper.activateDefaultTyping(
                                        LaissezFaireSubTypeValidator.instance,
                                        ObjectMapper.DefaultTyping.NON_FINAL);

                        GenericJackson2JsonRedisSerializer serializer = new GenericJackson2JsonRedisSerializer(mapper);

                        // Key serialization (String)
                        template.setKeySerializer(new StringRedisSerializer());
                        template.setHashKeySerializer(new StringRedisSerializer());

                        // Value serialization (JSON with type info)
                        template.setValueSerializer(serializer);
                        template.setHashValueSerializer(serializer);

                        template.afterPropertiesSet();
                        return template;
                } catch (Exception e) {
                        log.error("❌ Failed to create RedisTemplate: {}. Caching operations will be skipped.",
                                        e.getMessage());
                        log.debug("RedisTemplate creation error details:", e);
                        return null;
                }
        }

        /**
         * RedisCacheManager for Spring Cache annotations
         * Used by @Cacheable, @CacheEvict in services
         * Returns a no-op cache manager if Redis connection fails
         */
        @Bean
        public RedisCacheManager cacheManager(RedisConnectionFactory connectionFactory) {
                try {
                        // Default cache configuration
                        RedisCacheConfiguration defaultConfig = RedisCacheConfiguration.defaultCacheConfig()
                                        .entryTtl(Duration.ofMinutes(10))
                                        .serializeKeysWith(RedisSerializationContext.SerializationPair
                                                        .fromSerializer(new StringRedisSerializer()))
                                        .serializeValuesWith(RedisSerializationContext.SerializationPair
                                                        .fromSerializer(new GenericJackson2JsonRedisSerializer()))
                                        .disableCachingNullValues();

                        // Custom TTL per cache name (for Spring Cache)
                        Map<String, RedisCacheConfiguration> cacheConfigs = new HashMap<>();
                        cacheConfigs.put("menuItems", defaultConfig.entryTtl(Duration.ofMinutes(15)));
                        cacheConfigs.put("menuItem", defaultConfig.entryTtl(Duration.ofMinutes(15)));
                        cacheConfigs.put("menuItemsByCategory", defaultConfig.entryTtl(Duration.ofMinutes(15)));
                        cacheConfigs.put("menuItemsByCategoryId", defaultConfig.entryTtl(Duration.ofMinutes(15)));
                        cacheConfigs.put("categories", defaultConfig.entryTtl(Duration.ofMinutes(30)));
                        cacheConfigs.put("category", defaultConfig.entryTtl(Duration.ofMinutes(30)));
                        cacheConfigs.put("vouchers", defaultConfig.entryTtl(Duration.ofMinutes(5)));
                        cacheConfigs.put("tables", defaultConfig.entryTtl(Duration.ofMinutes(10)));

                        return RedisCacheManager.builder(connectionFactory)
                                        .cacheDefaults(defaultConfig)
                                        .withInitialCacheConfigurations(cacheConfigs)
                                        .build();
                } catch (Exception e) {
                        log.error("❌ Failed to create RedisCacheManager: {}. Caching will be disabled.",
                                        e.getMessage());
                        log.debug("RedisCacheManager creation error details:", e);
                        // Return a no-op cache manager by using default configuration
                        return RedisCacheManager.builder(connectionFactory)
                                        .cacheDefaults(RedisCacheConfiguration.defaultCacheConfig()
                                                        .disableCachingNullValues())
                                        .build();
                }
        }
}
