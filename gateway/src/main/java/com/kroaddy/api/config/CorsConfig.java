package com.kroaddy.api.config;

import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.Arrays;

@Configuration
public class CorsConfig implements WebMvcConfigurer {

    /**
     * CORS 설정 (공통)
     * WebMvcConfigurer와 CorsFilter 둘 다에 동일한 설정 적용
     */
    private CorsConfiguration getCorsConfiguration() {
        CorsConfiguration corsConfig = new CorsConfiguration();

        // 허용할 Origin 목록
        // allowCredentials: true일 때는 와일드카드(*) 사용 불가
        corsConfig.setAllowedOriginPatterns(Arrays.asList(
                "https://www.hatchery.kr",
                "https://www-hatchery-kr.vercel.app",
                "https://*.vercel.app", // Vercel 프리뷰 배포 도메인
                "http://localhost:*",
                "http://127.0.0.1:*"));

        // 허용할 HTTP 메서드
        corsConfig.setAllowedMethods(Arrays.asList(
                "GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));

        // 허용할 헤더
        corsConfig.setAllowedHeaders(Arrays.asList("*"));

        // Credentials 허용 (쿠키 포함 필수)
        corsConfig.setAllowCredentials(true);

        // 노출할 헤더
        corsConfig.setExposedHeaders(Arrays.asList("*"));

        // Preflight 요청 캐시 시간 (1시간)
        corsConfig.setMaxAge(3600L);

        return corsConfig;
    }

    /**
     * WebMvcConfigurer를 통한 CORS 설정
     * Spring MVC 레벨에서 CORS 처리 (가장 확실한 방법)
     */
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOriginPatterns(
                        "https://www.hatchery.kr",
                        "https://www-hatchery-kr.vercel.app",
                        "https://*.vercel.app",
                        "http://localhost:*",
                        "http://127.0.0.1:*")
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH")
                .allowedHeaders("*")
                .allowCredentials(true)
                .exposedHeaders("*")
                .maxAge(3600);
    }

    /**
     * CorsFilter Bean (필터 레벨에서도 CORS 처리)
     * 필터 순서를 최우선으로 설정하여 다른 필터보다 먼저 실행되도록 함
     */
    @Bean
    public FilterRegistrationBean<CorsFilter> corsFilterRegistration() {
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", getCorsConfiguration());

        FilterRegistrationBean<CorsFilter> registration = new FilterRegistrationBean<>(
                new CorsFilter(source));

        // 필터 순서를 최우선으로 설정 (가장 먼저 실행)
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE);

        return registration;
    }

    /**
     * 기존 CorsFilter Bean (하위 호환성 유지)
     * 
     * @deprecated FilterRegistrationBean을 사용하는 것이 더 확실함
     */
    @Bean
    @Deprecated
    public CorsFilter corsFilter() {
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", getCorsConfiguration());
        return new CorsFilter(source);
    }
}
