package com.kroaddy.api.log;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.kroaddy.api.log.dto.LogRequest;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

@RestController
@RequestMapping("/api/log")
public class LogController {

    /**
     * 로그인 관련 로그 기록
     * 프론트엔드의 logLoginAction 및 handleLoginSuccess에서 호출됨
     * 
     * @param request     LogRequest (action, url, tokenLength)
     * @param httpRequest HttpServletRequest (쿠키에서 토큰 추출용)
     * @return 성공/실패 응답
     */
    @PostMapping("/login")
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

            return ResponseEntity.status(500).body(response);
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
