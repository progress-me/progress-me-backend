package com.nhavronskyi.progressmebackend.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cache.annotation.CachingConfigurer;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.interceptor.CacheErrorHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJacksonJsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.DefaultTyping;
import tools.jackson.databind.JavaType;
import tools.jackson.databind.jsontype.BasicPolymorphicTypeValidator;
import tools.jackson.databind.jsontype.PolymorphicTypeValidator;
import tools.jackson.databind.jsontype.impl.DefaultTypeResolverBuilder;

import java.time.Duration;
import java.util.Map;

@Configuration
@EnableCaching
@ConditionalOnProperty(name = "spring.cache.type", havingValue = "redis")
public class RedisCacheConfig implements CachingConfigurer {

    @Bean
    public RedisCacheManager cacheManager(RedisConnectionFactory connectionFactory) {
        GenericJacksonJsonRedisSerializer jsonSerializer = createJsonSerializer();

        RedisCacheConfiguration defaultConfig = RedisCacheConfiguration.defaultCacheConfig()
                .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(jsonSerializer))
                .disableCachingNullValues();

        Map<String, RedisCacheConfiguration> cacheConfigs = Map.of(
                "users", defaultConfig.entryTtl(Duration.ofMinutes(20)),
                "dashboard", defaultConfig.entryTtl(Duration.ofMinutes(5)),
                "categories", defaultConfig.entryTtl(Duration.ofMinutes(30)),
                "persona", defaultConfig.entryTtl(Duration.ofHours(1)),
                "ai-settings", defaultConfig.entryTtl(Duration.ofHours(1))
        );

        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(defaultConfig.entryTtl(Duration.ofMinutes(10)))
                .withInitialCacheConfigurations(cacheConfigs)
                .build();
    }

    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);
        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(createJsonSerializer());
        return template;
    }

    @Override
    public CacheErrorHandler errorHandler() {
        return new LoggingCacheErrorHandler();
    }

    private GenericJacksonJsonRedisSerializer createJsonSerializer() {
        PolymorphicTypeValidator ptv = BasicPolymorphicTypeValidator.builder()
                .allowIfBaseType(Object.class)
                .allowIfSubType("com.nhavronskyi.progressmebackend.")
                .allowIfSubType("java.")
                .build();

        DefaultTypeResolverBuilder typer = new DefaultTypeResolverBuilder(
                ptv, DefaultTyping.NON_FINAL, JsonTypeInfo.As.WRAPPER_ARRAY) {
            @Override
            public boolean useForType(JavaType javaType) {
                if (javaType.isJavaLangObject()) {
                    return true;
                }
                // Include final types (records) unless they're in java.* package
                if (javaType.isFinal()
                        && !javaType.getRawClass().getPackageName().startsWith("java")) {
                    return true;
                }
                return super.useForType(javaType);
            }
        };

        return GenericJacksonJsonRedisSerializer.builder()
                .customize(builder -> builder
                        .setDefaultTyping(typer)
                        .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false))
                .build();
    }
}
