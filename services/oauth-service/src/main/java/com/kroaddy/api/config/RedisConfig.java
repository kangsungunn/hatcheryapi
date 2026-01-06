package com.kroaddy.api.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceClientConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;

@Configuration
public class RedisConfig {

    @Value("${spring.data.redis.host:localhost}")
    private String redisHost;

    @Value("${spring.data.redis.port:6379}")
    private int redisPort;

    @Value("${spring.data.redis.password:}")
    private String redisPassword;

    @Value("${REDIS_SSL:false}")
    private boolean redisSsl;

    /**
     * Redis 연결 팩토리 생성
     * Upstash를 사용하는 경우 REDIS_SSL=true 환경 변수 설정 필요
     */
    @Bean
    public RedisConnectionFactory redisConnectionFactory() {
        RedisStandaloneConfiguration config = new RedisStandaloneConfiguration();
        config.setHostName(redisHost);
        config.setPort(redisPort);
        
        if (redisPassword != null && !redisPassword.isEmpty()) {
            config.setPassword(redisPassword);
        }

        // Lettuce 클라이언트 설정
        LettuceClientConfiguration.LettuceClientConfigurationBuilder clientConfigBuilder = 
            LettuceClientConfiguration.builder()
                .commandTimeout(Duration.ofSeconds(2))
                .shutdownTimeout(Duration.ofMillis(100));

        // Upstash는 TLS를 사용하므로 SSL 설정
        if (redisSsl) {
            clientConfigBuilder.useSsl();
        }

        LettuceClientConfiguration clientConfig = clientConfigBuilder.build();
        
        return new LettuceConnectionFactory(config, clientConfig);
    }

    /**
     * RedisTemplate Bean 생성
     */
    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);
        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(new StringRedisSerializer());
        template.setHashKeySerializer(new StringRedisSerializer());
        template.setHashValueSerializer(new StringRedisSerializer());
        template.afterPropertiesSet();
        return template;
    }
}

