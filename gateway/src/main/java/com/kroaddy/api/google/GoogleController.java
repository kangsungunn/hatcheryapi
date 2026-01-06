package com.kroaddy.api.google;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.kroaddy.api.google.dto.GoogleUserInfo;
import com.kroaddy.api.jwt.JwtTokenProvider;

import jakarta.servlet.http.HttpServletResponse;
import jakarta.annotation.PostConstruct;
import org.springframework.http.ResponseCookie;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/api/auth/google")
public class GoogleController {

    private final GoogleService googleService;
    private final JwtTokenProvider jwtTokenProvider;
    private final RedisTemplate<String, Object> redisTemplate;

    @Value("${frontend.login-callback-url:http://localhost:3000}")
    private String frontendCallbackUrl;

    @Value("${frontend.login-success-path:/}")
    private String loginSuccessPath;

    @Value("${cookie.secure:false}")
    private boolean cookieSecure;

    @Value("${cookie.same-site:Lax}")
    private String cookieSameSite;

    @Autowired
    public GoogleController(GoogleService googleService, JwtTokenProvider jwtTokenProvider,
            RedisTemplate<String, Object> redisTemplate) {
        this.googleService = googleService;
        this.jwtTokenProvider = jwtTokenProvider;
        this.redisTemplate = redisTemplate;
        // 주의: 생성자 시점에는 @Value 주입이 아직 완료되지 않음
        // 실제 값은 @PostConstruct에서 확인 가능
    }

    /**
     * 환경 변수 진단 및 초기화 확인
     * 
     * @PostConstruct 시점에는 @Value 주입이 완료되어 있음
     */
    @PostConstruct
    public void checkEnv() {
        System.out.println("\n" + "=".repeat(80));
        System.out.println("[구글 컨트롤러] 환경 변수 초기화 확인");
        System.out.println("=".repeat(80));

        // Spring @Value로 주입된 값 확인 (이 시점에는 주입 완료됨)
        System.out.println("✅ [@Value] frontend.login-callback-url: " + frontendCallbackUrl);

        if (frontendCallbackUrl == null || frontendCallbackUrl.trim().isEmpty() || "null".equals(frontendCallbackUrl)) {
            System.err.println("❌ 경고: frontendCallbackUrl이 null입니다. 기본값(http://localhost:3000)을 사용합니다.");
        } else {
            System.out.println("✅ frontendCallbackUrl이 정상적으로 설정되었습니다: " + frontendCallbackUrl);
        }

        System.out.println("=".repeat(80) + "\n");
    }

    /**
     * 구글 인가 URL 생성 및 반환
     * 프론트엔드에서 이 URL로 리다이렉트
     * 
     * @return 구글 인가 URL
     */
    @GetMapping("/login")
    public ResponseEntity<?> getGoogleAuthUrl() {
        try {
            String authUrl = googleService.getAuthorizationUrl();
            if (authUrl == null || authUrl.isEmpty()) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(Map.of("success", false, "message",
                                "구글 인가 URL 생성 실패: 설정이 올바르지 않습니다. GOOGLE_CLIENT_ID를 확인하세요."));
            }
            Map<String, String> response = new HashMap<>();
            response.put("authUrl", authUrl);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            System.err.println("구글 인가 URL 생성 중 오류 발생: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("success", false, "message",
                            "구글 인가 URL 생성 실패: " + e.getMessage() +
                                    ". GOOGLE_CLIENT_ID 및 GOOGLE_CLIENT_SECRET 환경 변수를 확인하세요."));
        }
    }

    /**
     * 구글 인가 코드 콜백 처리
     * 1. 인가 코드로 액세스 토큰 요청
     * 2. 액세스 토큰으로 사용자 정보 요청
     * 3. JWT 발급 (구글 ID 기반)
     * 4. JWT를 쿠키에 설정하여 프론트엔드로 리다이렉트
     * 
     * @param code     구글 인가 코드
     * @param response HttpServletResponse (쿠키 설정용)
     * @return 프론트엔드로 리다이렉트(쿠키에 JWT 토큰 포함)
     */
    @GetMapping("/callback")
    public ResponseEntity<?> googleCallback(@RequestParam("code") String code,
            HttpServletResponse response) {
        try {
            // 1. 인가 코드로 액세스 토큰 요청
            var tokenResponse = googleService.getAccessToken(code);
            if (tokenResponse == null || tokenResponse.getAccessToken() == null) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(Map.of("success", false, "message", "구글 토큰 요청 실패"));
            }

            String accessToken = tokenResponse.getAccessToken();

            // 2. 액세스 토큰으로 사용자 정보 요청
            GoogleUserInfo userInfo = googleService.getUserInfo(accessToken);
            if (userInfo == null || userInfo.getId() == null) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(Map.of("success", false, "message", "구글 사용자 정보 조회 실패"));
            }

            // 3. 구글 ID 추출
            String googleId = userInfo.getId();

            // 4. JWT 및 Refresh Token 발급 (구글 ID를 subject로 사용)
            String jwt = jwtTokenProvider.generateToken(googleId);
            String refreshToken = jwtTokenProvider.generateRefreshToken(googleId);

            // 4-1. 백엔드 콘솔에 토큰 출력
            String timestamp = LocalDateTime.now()
                    .format(DateTimeFormatter.ofPattern("yyyy. MM. dd. a h:mm:ss", Locale.KOREAN));

            System.out.println("\n" + "=".repeat(60));
            System.out.println("[" + timestamp + "] 신규 구글 로그인 성공");
            System.out.println("User ID: " + googleId);
            System.out.println("JWT Token: " + jwt);
            System.out.println("Token Length: " + jwt.length());
            System.out.println("Refresh Token: " + refreshToken);
            System.out.println("Refresh Token Length: " + refreshToken.length());
            System.out.println("=".repeat(60) + "\n");

            // 4-2. Redis에 세션 정보 저장 (Upstash)
            try {
                String sessionKey = "session:" + googleId;
                Map<String, String> sessionData = new HashMap<>();
                sessionData.put("userId", googleId);
                sessionData.put("provider", "google");
                sessionData.put("loginTime", LocalDateTime.now().toString());
                sessionData.put("refreshToken", refreshToken);

                // 세션 데이터를 JSON 문자열로 저장 (7일 만료)
                redisTemplate.opsForValue().set(sessionKey, sessionData.toString(),
                        jwtTokenProvider.getRefreshExpiration() / 1000, TimeUnit.SECONDS);

                System.out.println("✅ Redis에 세션 저장 완료: " + sessionKey);
                System.out.println("   만료 시간: " + (jwtTokenProvider.getRefreshExpiration() / 1000) + "초 (7일)");
            } catch (Exception e) {
                System.err.println("⚠️ Redis 세션 저장 실패: " + e.getMessage());
                // Redis 저장 실패해도 로그인은 계속 진행
            }

            // 5. Access Token을 쿠키에 설정(ResponseCookie로 SameSite 명시적으로 설정)
            ResponseCookie accessTokenCookie = ResponseCookie.from("Authorization", jwt)
                    .httpOnly(true) // JavaScript 접근 차단 (XSS 방지)
                    .secure(cookieSecure) // HTTPS에서만 전송 (프로덕션: true)
                    .path("/") // 모든 경로에서 사용 가능
                    .maxAge(jwtTokenProvider.getExpiration() / 1000) // 초 단위
                    .sameSite(cookieSameSite.equals("None") ? "None" : cookieSameSite) // Lax, Strict, None
                    .build();
            response.addHeader(HttpHeaders.SET_COOKIE, accessTokenCookie.toString());

            // 5-1. Refresh Token을 쿠키에 설정(ResponseCookie로 SameSite 명시적으로 설정)
            ResponseCookie refreshTokenCookie = ResponseCookie.from("RefreshToken", refreshToken)
                    .httpOnly(true) // JavaScript 접근 차단 (XSS 방지)
                    .secure(cookieSecure) // HTTPS에서만 전송 (프로덕션: true)
                    .path("/") // 모든 경로에서 사용 가능
                    .maxAge(jwtTokenProvider.getRefreshExpiration() / 1000) // 초 단위 (7일 만료 시간)
                    .sameSite(cookieSameSite.equals("None") ? "None" : cookieSameSite) // Lax, Strict, None
                    .build();
            response.addHeader(HttpHeaders.SET_COOKIE, refreshTokenCookie.toString());

            // 6. 프론트엔드 콜백 페이지로 리다이렉트(토큰 포함 URL)
            // frontendCallbackUrl 값 확인 및 정규화
            String actualCallbackUrl = frontendCallbackUrl;
            if (actualCallbackUrl == null || actualCallbackUrl.trim().isEmpty() || "null".equals(actualCallbackUrl)) {
                System.err.println("⚠️ [구글 로그인] frontendCallbackUrl이 null이거나 비어있습니다. 기본값 사용: http://localhost:3000");
                actualCallbackUrl = "http://localhost:3000";
            }
            String normalizedCallbackUrl = actualCallbackUrl.trim();
            if (normalizedCallbackUrl.endsWith("/")) {
                normalizedCallbackUrl = normalizedCallbackUrl.substring(0, normalizedCallbackUrl.length() - 1);
            }
            String redirectUrl = normalizedCallbackUrl + "/login/google/callback";

            System.out.println("\n" + "=".repeat(80));
            System.out.println("[구글 로그인] 리다이렉트 정보");
            System.out.println("=".repeat(80));
            System.out.println("frontendCallbackUrl 원본: " + frontendCallbackUrl);
            System.out.println("frontendCallbackUrl 정규화: " + normalizedCallbackUrl);
            System.out.println("최종 리다이렉트 URL: " + redirectUrl);
            System.out.println("HTTP 상태 코드: 302 (FOUND)");
            System.out.println("Location 헤더: " + redirectUrl);
            System.out.println("=".repeat(80) + "\n");

            return ResponseEntity.status(HttpStatus.FOUND)
                    .header(HttpHeaders.LOCATION, redirectUrl)
                    .build();

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("success", false, "message",
                            "구글 로그인 처리 중 오류: " + e.getMessage()));
        }
    }
}
