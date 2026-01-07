package com.kroaddy.api.service;

import com.kroaddy.api.entity.LoginLog;
import com.kroaddy.api.repository.LoginLogRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class LoginLogService {
    
    private final LoginLogRepository loginLogRepository;
    
    /**
     * 로그인 로그 저장 (Refresh Token 포함)
     * 
     * @param userId 사용자 ID
     * @param provider 로그인 제공자 (google, kakao, naver)
     * @param accessToken Access Token
     * @param refreshToken Refresh Token
     * @param request HttpServletRequest (IP, User-Agent 추출용)
     */
    @Transactional
    public void saveLoginLog(String userId, String provider, String accessToken, String refreshToken, HttpServletRequest request) {
        try {
            System.out.println("\n" + "=".repeat(80));
            System.out.println("[LoginLogService] 로그인 로그 저장 시작");
            System.out.println("=".repeat(80));
            System.out.println("userId: " + userId);
            System.out.println("provider: " + provider);
            System.out.println("accessToken 길이: " + (accessToken != null ? accessToken.length() : "null"));
            System.out.println("refreshToken 길이: " + (refreshToken != null ? refreshToken.length() : "null"));
            
            // 데이터베이스 연결 정보 확인 (디버깅용)
            try {
                var connection = loginLogRepository.getClass().getClassLoader();
                System.out.println("[DEBUG] Repository 클래스: " + loginLogRepository.getClass().getName());
            } catch (Exception debugEx) {
                // 무시
            }
            
            // IP 주소 추출
            String ipAddress = getClientIpAddress(request);
            System.out.println("IP 주소: " + ipAddress);
            
            // User-Agent 추출
            String userAgent = request.getHeader("User-Agent");
            if (userAgent != null && userAgent.length() > 500) {
                userAgent = userAgent.substring(0, 500);
            }
            System.out.println("User-Agent: " + (userAgent != null ? userAgent.substring(0, Math.min(50, userAgent.length())) + "..." : "null"));
            
            // 로그 엔티티 생성
            LoginLog loginLog = LoginLog.builder()
                    .userId(userId)
                    .provider(provider)
                    .loginTime(LocalDateTime.now())
                    .ipAddress(ipAddress)
                    .userAgent(userAgent != null ? userAgent : "Unknown")
                    .accessToken(accessToken)
                    .refreshToken(refreshToken)
                    .build();
            
            System.out.println("LoginLog 엔티티 생성 완료");
            System.out.println("Repository에 저장 시도...");
            
            // 데이터베이스에 저장
            LoginLog savedLog = loginLogRepository.save(loginLog);
            
            System.out.println("✅ 로그인 로그 저장 완료!");
            System.out.println("저장된 ID: " + savedLog.getId());
            System.out.println("userId: " + savedLog.getUserId());
            System.out.println("provider: " + savedLog.getProvider());
            System.out.println("loginTime: " + savedLog.getLoginTime());
            System.out.println("=".repeat(80) + "\n");
            
            log.info("✅ 로그인 로그 저장 완료: userId={}, provider={}, ip={}, id={}", userId, provider, ipAddress, savedLog.getId());
        } catch (Exception e) {
            System.err.println("\n" + "=".repeat(80));
            System.err.println("[LoginLogService] ❌ 로그인 로그 저장 실패!");
            System.err.println("=".repeat(80));
            System.err.println("userId: " + userId);
            System.err.println("provider: " + provider);
            System.err.println("에러 메시지: " + e.getMessage());
            System.err.println("에러 클래스: " + e.getClass().getName());
            System.err.println("스택 트레이스:");
            e.printStackTrace();
            System.err.println("=".repeat(80) + "\n");
            
            log.error("⚠️ 로그인 로그 저장 실패: userId={}, provider={}, error={}", userId, provider, e.getMessage(), e);
            // 로그 저장 실패해도 로그인은 계속 진행
        }
    }
    
    /**
     * 클라이언트 IP 주소 추출
     * 프록시나 로드 밸런서를 통한 요청도 처리
     */
    private String getClientIpAddress(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        // 여러 IP가 있을 경우 첫 번째 IP 사용
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip != null ? ip : "Unknown";
    }
}

