package com.kroaddy.api.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "login_logs", schema = "public")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoginLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String userId; // OAuth 제공자 ID (google, kakao, naver)

    @Column(nullable = false)
    private String provider; // "google", "kakao", "naver"

    @Column(nullable = false)
    private LocalDateTime loginTime;

    @Column(length = 500)
    private String userAgent; // 브라우저 정보

    @Column(length = 50)
    private String ipAddress; // IP 주소

    // Refresh Token 필드
    @Column(name = "refresh_token", length = 1000)
    private String refreshToken; // Refresh Token 저장 (Access Token은 Redis에만 저장)

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (loginTime == null) {
            loginTime = LocalDateTime.now();
        }
    }
}
