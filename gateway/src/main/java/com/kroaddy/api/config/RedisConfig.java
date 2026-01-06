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
import jakarta.annotation.PostConstruct;

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

    private RedisConnectionFactory connectionFactory;

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
        
        LettuceConnectionFactory factory = new LettuceConnectionFactory(config, clientConfig);
        this.connectionFactory = factory;
        
        return factory;
    }

    /**
     * Redis 연결 테스트 및 로그 출력
     */
    @PostConstruct
    public void testRedisConnection() {
        try {
            System.out.println("\n" + "=".repeat(80));
            System.out.println("[Redis 설정] Upstash 연결 정보 확인");
            System.out.println("=".repeat(80));
            System.out.println("Redis Host: " + redisHost);
            System.out.println("Redis Port: " + redisPort);
            System.out.println("Redis SSL: " + redisSsl);
            System.out.println("Redis Password: " + (redisPassword != null && !redisPassword.isEmpty() ? "설정됨 (길이: " + redisPassword.length() + ")" : "없음"));
            
            if (connectionFactory != null) {
                // 연결 테스트
                var connection = connectionFactory.getConnection();
                try {
                    connection.ping();
                    System.out.println("✅ Redis 연결 성공! (Upstash)");
                    System.out.println("✅ PING 명령어 응답: PONG");
                } catch (Exception e) {
                    System.err.println("❌ Redis 연결 실패: " + e.getMessage());
                    e.printStackTrace();
                } finally {
                    connection.close();
                }
            } else {
                System.err.println("⚠️ RedisConnectionFactory가 아직 초기화되지 않았습니다.");
            }
            System.out.println("=".repeat(80) + "\n");
        } catch (Exception e) {
            System.err.println("❌ Redis 연결 테스트 중 오류 발생: " + e.getMessage());
            e.printStackTrace();
        }
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

