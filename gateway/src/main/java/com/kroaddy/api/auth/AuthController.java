package com.kroaddy.api.auth;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.kroaddy.api.jwt.JwtTokenProvider;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import com.kroaddy.api.log.dto.LogRequest;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class AuthController {

    private final JwtTokenProvider jwtTokenProvider;

    @Value("${cookie.secure:false}")
    private boolean cookieSecure;

    @Value("${cookie.same-site:Lax}")
    private String cookieSameSite;

    @Autowired
    public AuthController(JwtTokenProvider jwtTokenProvider) {
        this.jwtTokenProvider = jwtTokenProvider;
    }

    /**
     * 인증 상태 확인 및 사용자 정보 반환
     * 쿠키에서 JWT 토큰을 읽어 검증하고 사용자 ID를 반환
     * 
     * @param request HttpServletRequest (쿠키 읽기용)
     * @return 사용자 정보 또는 에러 응답
     */
    @GetMapping("/auth/me")
    public ResponseEntity<?> getCurrentUser(HttpServletRequest request) {
        try {
            // 쿠키에서 토큰 추출
            String token = extractTokenFromCookie(request);

            if (token == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of(
                                "error", "Unauthorized",
                                "message", "인증이 필요합니다."));
            }

            // 토큰 검증
            if (!jwtTokenProvider.validateToken(token)) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of(
                                "error", "Unauthorized",
                                "message", "유효하지 않은 토큰입니다."));
            }

            // 토큰에서 사용자 ID 추출
            String userId = jwtTokenProvider.getSubjectFromToken(token);

            // 사용자 정보 반환
            // TODO: 실제 사용자 정보를 DB에서 조회하거나 소셜 로그인 정보를 조회하는 로직 추가 필요
            Map<String, Object> userInfo = new HashMap<>();
            userInfo.put("id", userId);
            // userInfo.put("email", user.getEmail());
            // userInfo.put("name", user.getName());
            // userInfo.put("profileImage", user.getProfileImage());

            return ResponseEntity.ok(userInfo);

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of(
                            "error", "Internal Server Error",
                            "message", "서버 오류가 발생했습니다: " + e.getMessage()));
        }
    }

    /**
     * 토큰 갱신
     * Refresh Token으로 새로운 Access Token 발급
     * 
     * @param request  HttpServletRequest (쿠키 읽기용)
     * @param response HttpServletResponse (쿠키 설정용)
     * @return 새로운 Access Token 또는 에러 응답
     */
    @PostMapping("/auth/refresh")
    public ResponseEntity<?> refreshToken(HttpServletRequest request,
            HttpServletResponse response) {
        try {
            // 쿠키에서 Refresh Token 추출
            String refreshToken = extractRefreshTokenFromCookie(request);

            if (refreshToken == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of(
                                "error", "Unauthorized",
                                "message", "Refresh Token이 필요합니다."));
            }

            // Refresh Token 검증
            if (!jwtTokenProvider.validateToken(refreshToken)) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of(
                                "error", "Unauthorized",
                                "message", "유효하지 않은 Refresh Token입니다."));
            }

            // Refresh Token에서 사용자 ID 추출
            String userId = jwtTokenProvider.getSubjectFromToken(refreshToken);

            // 새로운 Access Token 발급
            String newAccessToken = jwtTokenProvider.generateToken(userId);

            // 새로운 Access Token을 쿠키에 설정
            ResponseCookie accessTokenCookie = ResponseCookie.from("Authorization", newAccessToken)
                    .httpOnly(true)
                    .secure(cookieSecure)
                    .path("/")
                    .maxAge(jwtTokenProvider.getExpiration() / 1000)
                    .sameSite(cookieSameSite.equals("None") ? "None" : cookieSameSite)
                    .build();
            response.addHeader(HttpHeaders.SET_COOKIE, accessTokenCookie.toString());

            Map<String, Object> responseBody = new HashMap<>();
            responseBody.put("success", true);
            responseBody.put("message", "토큰이 갱신되었습니다.");

            return ResponseEntity.ok(responseBody);

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of(
                            "error", "Internal Server Error",
                            "message", "토큰 갱신 중 오류가 발생했습니다: " + e.getMessage()));
        }
    }

    /**
     * 로그인 관련 로그 기록
     * 프론트엔드의 logLoginAction 및 handleLoginSuccess에서 호출됨
     * 
     * @param request     LogRequest (action, url, tokenLength)
     * @param httpRequest HttpServletRequest (쿠키에서 토큰 추출용)
     * @return 성공/실패 응답
     */
    @PostMapping("/log/login")
    public ResponseEntity<Map<String, Object>> logLogin(@RequestBody LogRequest request,
            HttpServletRequest httpRequest) {
        try {
            String timestamp = LocalDateTime.now()
                    .format(DateTimeFormatter.ofPattern("yyyy. MM. dd. a h:mm:ss", Locale.KOREAN));

            // 쿠키에서 Access Token과 Refresh Token 추출
            String accessToken = extractTokenFromCookie(httpRequest);
            String refreshToken = extractRefreshTokenFromCookie(httpRequest);

            System.out.println("\n" + "=".repeat(60));
            System.out.println("[" + timestamp + "] 🔹 " + request.getAction());

            if (request.getUrl() != null && !request.getUrl().isEmpty()) {
                System.out.println("URL: " + request.getUrl());
            }

            if (request.getTokenLength() != null) {
                System.out.println("Token Length: " + request.getTokenLength());
            }

            // Access Token 출력
            if (accessToken != null) {
                System.out.println("Access Token: " + accessToken);
                System.out.println("Access Token Length: " + accessToken.length());
            } else {
                System.out.println("Access Token: 없음");
            }

            // Refresh Token 출력
            if (refreshToken != null) {
                System.out.println("Refresh Token: " + refreshToken);
                System.out.println("Refresh Token Length: " + refreshToken.length());
            } else {
                System.out.println("Refresh Token: 없음");
            }

            System.out.println("=".repeat(60) + "\n");

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "로그가 기록되었습니다.");

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            System.err.println("❌ 로그인 로그 기록 실패: " + e.getMessage());
            e.printStackTrace();

            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("error", "로그 기록 실패: " + e.getMessage());

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * 로그아웃
     * 쿠키에서 Access Token과 Refresh Token 삭제
     * 
     * @param request  HttpServletRequest
     * @param response HttpServletResponse (쿠키 삭제용)
     * @return 로그아웃 성공 응답
     */
    @PostMapping("/auth/logout")
    public ResponseEntity<Map<String, Object>> logout(HttpServletRequest request,
            HttpServletResponse response) {
        try {
            // Access Token 쿠키 삭제 (ResponseCookie로 SameSite 명시적으로 설정)
            ResponseCookie accessTokenCookie = ResponseCookie.from("Authorization", "")
                    .httpOnly(true)
                    .secure(cookieSecure)
                    .path("/")
                    .maxAge(0) // 즉시 삭제
                    .sameSite(cookieSameSite.equals("None") ? "None" : cookieSameSite) // Lax, Strict, None
                    .build();
            response.addHeader(HttpHeaders.SET_COOKIE, accessTokenCookie.toString());

            // Refresh Token 쿠키 삭제 (ResponseCookie로 SameSite 명시적으로 설정)
            ResponseCookie refreshTokenCookie = ResponseCookie.from("RefreshToken", "")
                    .httpOnly(true)
                    .secure(cookieSecure)
                    .path("/")
                    .maxAge(0) // 즉시 삭제
                    .sameSite(cookieSameSite.equals("None") ? "None" : cookieSameSite) // Lax, Strict, None
                    .build();
            response.addHeader(HttpHeaders.SET_COOKIE, refreshTokenCookie.toString());

            Map<String, Object> responseBody = new HashMap<>();
            responseBody.put("success", true);
            responseBody.put("message", "로그아웃되었습니다.");

            return ResponseEntity.ok(responseBody);

        } catch (Exception e) {
            Map<String, Object> responseBody = new HashMap<>();
            responseBody.put("success", false);
            responseBody.put("error", "로그아웃 처리 중 오류가 발생했습니다: " + e.getMessage());

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(responseBody);
        }
    }

    /**
     * 쿠키에서 Authorization 토큰 추출
     * 
     * @param request HttpServletRequest
     * @return JWT 토큰 또는 null
     */
    private String extractTokenFromCookie(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if ("Authorization".equals(cookie.getName())) {
                    return cookie.getValue();
                }
            }
        }
        return null;
    }

    /**
     * 쿠키에서 Refresh Token 추출
     * 
     * @param request HttpServletRequest
     * @return Refresh Token 또는 null
     */
    private String extractRefreshTokenFromCookie(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if ("RefreshToken".equals(cookie.getName())) {
                    return cookie.getValue();
                }
            }
        }
        return null;
    }
}
