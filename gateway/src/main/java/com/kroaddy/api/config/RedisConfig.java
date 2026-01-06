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
     * Redis Host 정규화 (http:// 또는 https:// 제거)
     */
    private String normalizeHost(String host) {
        if (host == null) {
            return "localhost";
        }
        String normalized = host.trim();
        // http:// 또는 https:// 제거
        if (normalized.startsWith("http://")) {
            normalized = normalized.substring(7);
        } else if (normalized.startsWith("https://")) {
            normalized = normalized.substring(8);
        }
        // 마지막 슬래시 제거
        if (normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        return normalized;
    }

    /**
     * Redis 연결 팩토리 생성
     * Upstash를 사용하는 경우 REDIS_SSL=true 환경 변수 설정 필요
     */
    @Bean
    public RedisConnectionFactory redisConnectionFactory() {
        // Host 정규화 (http:// 제거)
        String normalizedHost = normalizeHost(redisHost);
        
        RedisStandaloneConfiguration config = new RedisStandaloneConfiguration();
        config.setHostName(normalizedHost);
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
        
        // 연결 정보 로그 출력
        System.out.println("\n" + "=".repeat(80));
        System.out.println("[Redis 설정] Upstash 연결 정보");
        System.out.println("=".repeat(80));
        System.out.println("Redis Host (원본): " + redisHost);
        System.out.println("Redis Host (정규화): " + normalizedHost);
        System.out.println("Redis Port: " + redisPort);
        System.out.println("Redis SSL: " + redisSsl);
        System.out.println("Redis Password: " + (redisPassword != null && !redisPassword.isEmpty() ? "설정됨 (길이: " + redisPassword.length() + ")" : "없음"));
        
        // Upstash 연결 확인
        if (normalizedHost.contains("upstash.io")) {
            System.out.println("✅ Upstash Redis 인스턴스로 확인됨");
        } else if (normalizedHost.equals("localhost") || normalizedHost.equals("127.0.0.1")) {
            System.out.println("⚠️ 로컬 Redis로 설정되어 있습니다 (Upstash 아님)");
        } else {
            System.out.println("ℹ️ Redis Host: " + normalizedHost);
        }
        System.out.println("=".repeat(80) + "\n");
        
        return factory;
    }

    /**
     * RedisTemplate Bean 생성
     * 생성 후 연결 테스트 수행
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
        
        // 연결 테스트
        try {
            String normalizedHost = normalizeHost(redisHost);
            var connection = connectionFactory.getConnection();
            try {
                String pingResult = connection.ping();
                System.out.println("\n" + "=".repeat(80));
                System.out.println("[Redis 연결 테스트]");
                System.out.println("=".repeat(80));
                System.out.println("✅ Redis 연결 성공!");
                System.out.println("✅ Host: " + normalizedHost);
                System.out.println("✅ PING 응답: " + pingResult);
                if (normalizedHost.contains("upstash.io")) {
                    System.out.println("✅ Upstash Redis에 연결되었습니다!");
                } else if (normalizedHost.equals("localhost") || normalizedHost.equals("127.0.0.1")) {
                    System.out.println("⚠️ 로컬 Redis에 연결되었습니다 (Upstash 아님)");
                } else {
                    System.out.println("ℹ️ Redis Host: " + normalizedHost);
                }
                System.out.println("=".repeat(80) + "\n");
            } catch (Exception e) {
                System.err.println("\n" + "=".repeat(80));
                System.err.println("[Redis 연결 테스트] 실패");
                System.err.println("=".repeat(80));
                System.err.println("❌ Redis 연결 실패: " + e.getMessage());
                System.err.println("Host: " + normalizedHost);
                e.printStackTrace();
                System.err.println("=".repeat(80) + "\n");
            } finally {
                connection.close();
            }
        } catch (Exception e) {
            System.err.println("❌ Redis 연결 테스트 중 오류: " + e.getMessage());
            e.printStackTrace();
        }
        
        return template;
    }
}

