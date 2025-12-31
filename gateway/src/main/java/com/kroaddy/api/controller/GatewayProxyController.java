package com.kroaddy.api.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import jakarta.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Map;

@RestController
public class GatewayProxyController {

    private final RestTemplate restTemplate;
    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    // 라우팅 설정
    private static final Map<String, RouteConfig> ROUTES = new HashMap<>();

    static {
        // OAuth 및 Auth는 이제 Gateway에 직접 구현됨 (모놀리식 구조)
        // /api/auth/** - Gateway의 AuthController, KakaoController, NaverController, GoogleController에서 처리
        // /api/log/** - Gateway의 LogController에서 처리
        
        // User-service 라우팅 (향후 통합 예정)
        ROUTES.put("/api/users/**", new RouteConfig("http://localhost:8082", "/api/users"));
        
        // AI Services 라우팅
        ROUTES.put("/api/ai/crawler/**", new RouteConfig("http://localhost:9001", "/api/ai/crawler"));
        ROUTES.put("/api/ai/rag/**", new RouteConfig("http://localhost:9004", "/api/ai/rag"));
        ROUTES.put("/api/ai/chatbot/**", new RouteConfig("http://localhost:9003", "/api/ai/chatbot"));
        ROUTES.put("/api/ai/auth/**", new RouteConfig("http://localhost:9002", "/api/ai/auth"));
        
        // ML Service 라우팅 (경로 재작성 포함)
        ROUTES.put("/api/ai/ml/**", new RouteConfig("http://localhost:9006", "/api/ai/ml", "/titanic"));
        ROUTES.put("/api/ai/titanic/**", new RouteConfig("http://localhost:9006", "/api/ai/titanic", "/titanic"));
        ROUTES.put("/api/ai/seoul/**", new RouteConfig("http://localhost:9006", "/api/ai/seoul", "/seoul"));
        ROUTES.put("/api/ml/usa/**", new RouteConfig("http://localhost:9006", "/api/ml/usa", "/usa"));
        ROUTES.put("/api/ml/nlp/**", new RouteConfig("http://localhost:9006", "/api/ml/nlp", "/nlp"));
        
        // Transformer Service 라우팅
        ROUTES.put("/api/ai/transformer/**", new RouteConfig("http://localhost:9007", "/api/ai/transformer", "/koelectra"));
        ROUTES.put("/transformer-docs/**", new RouteConfig("http://localhost:9007", "/transformer-docs", "/docs"));
        ROUTES.put("/transformer-openapi/**", new RouteConfig("http://localhost:9007", "/transformer-openapi", "/openapi.json"));
    }

    @Autowired
    public GatewayProxyController(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    // Gateway에서 직접 처리하지 않는 경로만 프록시
    // /api/auth/**와 /api/log/**는 Gateway 컨트롤러에서 직접 처리하므로 제외
    @RequestMapping(value = {
        "/api/users/**",
        "/api/ai/**",
        "/api/ml/**",
        "/transformer-docs/**",
        "/transformer-openapi/**"
    }, method = {RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT, RequestMethod.DELETE, RequestMethod.PATCH})
    public ResponseEntity<?> proxyRequest(
            HttpServletRequest request,
            @RequestBody(required = false) String body,
            @RequestHeader HttpHeaders headers) {
        
        String requestPath = request.getRequestURI();
        String queryString = request.getQueryString();
        
        // 라우팅 매칭
        RouteConfig routeConfig = findRoute(requestPath);
        if (routeConfig == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "Route not found", "path", requestPath));
        }
        
        // 타겟 URL 생성
        String targetPath = rewritePath(requestPath, routeConfig);
        String targetUrl = routeConfig.getBaseUrl() + targetPath;
        if (queryString != null) {
            targetUrl += "?" + queryString;
        }
        
        // 요청 헤더 복사 (호스트 제외)
        HttpHeaders requestHeaders = new HttpHeaders();
        headers.forEach((key, values) -> {
            if (!key.equalsIgnoreCase("host") && !key.equalsIgnoreCase("content-length")) {
                requestHeaders.put(key, values);
            }
        });
        
        // 요청 본문 설정
        HttpEntity<String> requestEntity = new HttpEntity<>(body, requestHeaders);
        
        try {
            // HTTP 메서드에 따라 요청 전달
            HttpMethod method = HttpMethod.valueOf(request.getMethod());
            ResponseEntity<String> response = restTemplate.exchange(
                    targetUrl,
                    method,
                    requestEntity,
                    String.class
            );
            
            // 응답 헤더 복사
            HttpHeaders responseHeaders = new HttpHeaders();
            response.getHeaders().forEach((key, values) -> {
                if (!key.equalsIgnoreCase("content-length")) {
                    responseHeaders.put(key, values);
                }
            });
            
            return ResponseEntity.status(response.getStatusCode())
                    .headers(responseHeaders)
                    .body(response.getBody());
                    
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Gateway error", "message", e.getMessage()));
        }
    }
    
    private RouteConfig findRoute(String path) {
        for (Map.Entry<String, RouteConfig> entry : ROUTES.entrySet()) {
            if (pathMatcher.match(entry.getKey(), path)) {
                return entry.getValue();
            }
        }
        return null;
    }
    
    private String rewritePath(String originalPath, RouteConfig routeConfig) {
        if (routeConfig.getRewritePath() != null) {
            // 경로 재작성: /api/ai/ml/samsung -> /titanic/samsung
            String originalPattern = routeConfig.getOriginalPath();
            if (originalPattern.endsWith("/**")) {
                String basePath = originalPattern.substring(0, originalPattern.length() - 3);
                if (originalPath.startsWith(basePath)) {
                    String remainingPath = originalPath.substring(basePath.length());
                    // 앞의 슬래시 제거
                    if (remainingPath.startsWith("/")) {
                        remainingPath = remainingPath.substring(1);
                    }
                    // 뒤의 슬래시 제거
                    if (remainingPath.endsWith("/")) {
                        remainingPath = remainingPath.substring(0, remainingPath.length() - 1);
                    }
                    if (remainingPath.isEmpty()) {
                        return routeConfig.getRewritePath();
                    }
                    return routeConfig.getRewritePath() + "/" + remainingPath;
                }
            }
            return routeConfig.getRewritePath();
        }
        
        // 경로 재작성 없이 원본 경로 사용
        return originalPath;
    }
    
    private static class RouteConfig {
        private final String baseUrl;
        private final String originalPath;
        private final String rewritePath;
        
        public RouteConfig(String baseUrl, String originalPath) {
            this(baseUrl, originalPath, null);
        }
        
        public RouteConfig(String baseUrl, String originalPath, String rewritePath) {
            this.baseUrl = baseUrl;
            this.originalPath = originalPath;
            this.rewritePath = rewritePath;
        }
        
        public String getBaseUrl() {
            return baseUrl;
        }
        
        public String getOriginalPath() {
            return originalPath;
        }
        
        public String getRewritePath() {
            return rewritePath;
        }
    }
}

