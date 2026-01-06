package com.kroaddy.api.naver;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.kroaddy.api.jwt.JwtTokenProvider;
import com.kroaddy.api.naver.dto.NaverUserInfo;

import jakarta.servlet.http.HttpServletResponse;
import jakarta.annotation.PostConstruct;
import org.springframework.http.ResponseCookie;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

@RestController
@RequestMapping("/api/auth/naver")
public class NaverController {

        private final NaverService naverService;
        private final JwtTokenProvider jwtTokenProvider;

        @Value("${frontend.login-callback-url:http://localhost:3000}")
        private String frontendCallbackUrl;

        @Value("${frontend.login-success-path:/}")
        private String loginSuccessPath;

        @Value("${cookie.secure:false}")
        private boolean cookieSecure;

        @Value("${cookie.same-site:Lax}")
        private String cookieSameSite;

        @Autowired
        public NaverController(NaverService naverService, JwtTokenProvider jwtTokenProvider) {
                this.naverService = naverService;
                this.jwtTokenProvider = jwtTokenProvider;
                System.out.println("[네이버 컨트롤러 초기화] frontendCallbackUrl: " + frontendCallbackUrl);
        }

        /**
         * 환경 변수 진단 (ChatGPT 제안)
         * 컨테이너 내부에서 환경 변수가 제대로 주입되었는지 확인
         */
        @PostConstruct
        public void checkEnv() {
                System.out.println("\n" + "=".repeat(80));
                System.out.println("[네이버 컨트롤러] 환경 변수 진단 시작");
                System.out.println("=".repeat(80));
                
                // 1. Spring @Value로 주입된 값 확인
                System.out.println("[@Value] frontend.login-callback-url: " + frontendCallbackUrl);
                
                // 2. System.getenv()로 직접 확인 (여러 가능한 변수명 시도)
                System.out.println("[System.getenv] FRONTEND_LOGIN_CALLBACK_URL: " + System.getenv("FRONTEND_LOGIN_CALLBACK_URL"));
                System.out.println("[System.getenv] FRONTEND_CALLBACK_URL: " + System.getenv("FRONTEND_CALLBACK_URL"));
                System.out.println("[System.getenv] frontend.login-callback-url: " + System.getenv("frontend.login-callback-url"));
                
                // 3. 모든 환경 변수에서 frontend 관련 찾기
                System.out.println("\n[모든 환경 변수에서 'FRONTEND' 검색]:");
                System.getenv().entrySet().stream()
                    .filter(entry -> entry.getKey().toUpperCase().contains("FRONTEND"))
                    .forEach(entry -> System.out.println("  " + entry.getKey() + " = " + entry.getValue()));
                
                // 4. 모든 환경 변수에서 callback 관련 찾기
                System.out.println("\n[모든 환경 변수에서 'CALLBACK' 검색]:");
                System.getenv().entrySet().stream()
                    .filter(entry -> entry.getKey().toUpperCase().contains("CALLBACK"))
                    .forEach(entry -> System.out.println("  " + entry.getKey() + " = " + entry.getValue()));
                
                System.out.println("=".repeat(80));
                System.out.println("[네이버 컨트롤러] 환경 변수 진단 완료\n");
        }

        /**
         * 네이버 인가 URL 생성 및 반환
         * 프론트엔드에서 이 URL로 리다이렉트
         * 
         * @return 네이버 인가 URL
         */
        @GetMapping("/login")
        public ResponseEntity<Map<String, String>> getNaverAuthUrl() {
                String authUrl = naverService.getAuthorizationUrl();
                Map<String, String> response = new HashMap<>();
                response.put("authUrl", authUrl);
                return ResponseEntity.ok(response);
        }

        /**
         * 네이버 인가 코드 콜백 처리
         * 1. 인가 코드로 액세스 토큰 요청
         * 2. 액세스 토큰으로 사용자 정보 요청
         * 3. JWT 발급 (네이버 ID 기반)
         * 4. JWT를 쿠키에 설정하여 프론트엔드로 리다이렉트
         * 
         * @param code     네이버 인가 코드
         * @param state    상태 값
         * @param response HttpServletResponse (쿠키 설정용)
         * @return 프론트엔드로 리다이렉트(쿠키에 JWT 토큰 포함)
         */
        @GetMapping("/callback")
        public ResponseEntity<?> naverCallback(@RequestParam("code") String code,
                        @RequestParam("state") String state,
                        HttpServletResponse response) {
                try {
                        // 1. 인가 코드로 액세스 토큰 요청
                        var tokenResponse = naverService.getAccessToken(code, state);
                        if (tokenResponse == null || tokenResponse.getAccessToken() == null) {
                                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                                .body(Map.of("success", false, "message", "네이버 토큰 요청 실패"));
                        }

                        String accessToken = tokenResponse.getAccessToken();

                        // 2. 액세스 토큰으로 사용자 정보 요청
                        NaverUserInfo userInfo = naverService.getUserInfo(accessToken);
                        if (userInfo == null || userInfo.getResponse() == null
                                        || userInfo.getResponse().getId() == null) {
                                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                                .body(Map.of("success", false, "message", "네이버 사용자 정보 조회 실패"));
                        }

                        // 3. 네이버 ID 추출
                        String naverId = userInfo.getResponse().getId();

                        // 4. JWT 및 Refresh Token 발급 (네이버 ID를 subject로 사용)
                        String jwt = jwtTokenProvider.generateToken(naverId);
                        String refreshToken = jwtTokenProvider.generateRefreshToken(naverId);

                        // 4-1. 백엔드 콘솔에 토큰 출력
                        String timestamp = LocalDateTime.now()
                                        .format(DateTimeFormatter.ofPattern("yyyy. MM. dd. a h:mm:ss", Locale.KOREAN));

                        System.out.println("\n" + "=".repeat(60));
                        System.out.println("[" + timestamp + "] 신규 네이버 로그인 성공");
                        System.out.println("User ID: " + naverId);
                        System.out.println("JWT Token: " + jwt);
                        System.out.println("Token Length: " + jwt.length());
                        System.out.println("Refresh Token: " + refreshToken);
                        System.out.println("Refresh Token Length: " + refreshToken.length());
                        System.out.println("=".repeat(60) + "\n");

                        // 5. Access Token을 쿠키에 설정(ResponseCookie로 SameSite 명시적으로 설정)
                        ResponseCookie accessTokenCookie = ResponseCookie.from("Authorization", jwt)
                                        .httpOnly(true) // JavaScript 접근 차단 (XSS 방지)
                                        .secure(cookieSecure) // HTTPS에서만 전송 (프로덕션: true)
                                        .path("/") // 모든 경로에서 사용 가능
                                        .maxAge(jwtTokenProvider.getExpiration() / 1000) // 초 단위
                                        .sameSite(cookieSameSite.equals("None") ? "None" : cookieSameSite) // Lax,
                                                                                                           // Strict,
                                                                                                           // None
                                        .build();
                        response.addHeader(HttpHeaders.SET_COOKIE, accessTokenCookie.toString());

                        // 5-1. Refresh Token을 쿠키에 설정(ResponseCookie로 SameSite 명시적으로 설정)
                        ResponseCookie refreshTokenCookie = ResponseCookie.from("RefreshToken", refreshToken)
                                        .httpOnly(true) // JavaScript 접근 차단 (XSS 방지)
                                        .secure(cookieSecure) // HTTPS에서만 전송 (프로덕션: true)
                                        .path("/") // 모든 경로에서 사용 가능
                                        .maxAge(jwtTokenProvider.getRefreshExpiration() / 1000) // 초 단위 (7일 만료 시간)
                                        .sameSite(cookieSameSite.equals("None") ? "None" : cookieSameSite) // Lax,
                                                                                                           // Strict,
                                                                                                           // None
                                        .build();
                        response.addHeader(HttpHeaders.SET_COOKIE, refreshTokenCookie.toString());

                        // 6. 프론트엔드 콜백 페이지로 리다이렉트(토큰 포함 URL)
                        // frontendCallbackUrl 값 확인 및 정규화
                        String actualCallbackUrl = frontendCallbackUrl;
                        if (actualCallbackUrl == null || actualCallbackUrl.trim().isEmpty() || "null".equals(actualCallbackUrl)) {
                                System.err.println("⚠️ [네이버 로그인] frontendCallbackUrl이 null이거나 비어있습니다. 기본값 사용: http://localhost:3000");
                                actualCallbackUrl = "http://localhost:3000";
                        }
                        String normalizedCallbackUrl = actualCallbackUrl.trim();
                        if (normalizedCallbackUrl.endsWith("/")) {
                            normalizedCallbackUrl = normalizedCallbackUrl.substring(0, normalizedCallbackUrl.length() - 1);
                        }
                        String redirectUrl = normalizedCallbackUrl + "/login/naver/callback";
                        
                        System.out.println("\n" + "=".repeat(60));
                        System.out.println("[네이버 로그인] frontendCallbackUrl 원본: " + frontendCallbackUrl);
                        System.out.println("[네이버 로그인] frontendCallbackUrl 정규화: " + normalizedCallbackUrl);
                        System.out.println("[네이버 로그인] 프론트엔드로 리다이렉트: " + redirectUrl);
                        System.out.println("=".repeat(60) + "\n");

                        return ResponseEntity.status(HttpStatus.FOUND)
                                        .header(HttpHeaders.LOCATION, redirectUrl)
                                        .build();

                } catch (Exception e) {
                        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                        .body(Map.of("success", false, "message",
                                                        "네이버 로그인 처리 중 오류: " + e.getMessage()));
                }
        }
}

