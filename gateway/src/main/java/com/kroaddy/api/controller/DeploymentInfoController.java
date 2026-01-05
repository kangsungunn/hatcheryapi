package com.kroaddy.api.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/deployment")
public class DeploymentInfoController {

    // 빌드 시점 정보 (환경변수 또는 application.yaml에서 주입)
    @Value("${app.build.time:${BUILD_TIME:Unknown}}")
    private String buildTime;

    @Value("${app.build.version:${BUILD_VERSION:dev}}")
    private String buildVersion;

    @Value("${app.git.commit:${GIT_COMMIT:Unknown}}")
    private String gitCommit;

    @Value("${app.git.branch:${GIT_BRANCH:main}}")
    private String gitBranch;

    /**
     * 배포 정보 조회
     * 빌드 시간, Git 커밋 정보 등을 반환하여 배포된 버전을 확인할 수 있음
     */
    @GetMapping("/info")
    public ResponseEntity<Map<String, Object>> getDeploymentInfo() {
        Map<String, Object> info = new HashMap<>();
        
        // 빌드 정보
        info.put("buildTime", buildTime);
        info.put("buildVersion", buildVersion);
        info.put("gitCommit", gitCommit);
        info.put("gitBranch", gitBranch);
        
        // 서버 실행 시간
        info.put("serverTime", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        
        // 애플리케이션 정보
        info.put("applicationName", "kroaddy-api");
        info.put("status", "running");
        
        return ResponseEntity.ok(info);
    }

    /**
     * 간단한 배포 확인용 엔드포인트
     * 배포 시간만 반환 (빠른 확인용)
     */
    @GetMapping("/check")
    public ResponseEntity<Map<String, String>> checkDeployment() {
        Map<String, String> response = new HashMap<>();
        response.put("status", "deployed");
        response.put("buildTime", buildTime);
        response.put("gitCommit", gitCommit.length() > 7 ? gitCommit.substring(0, 7) : gitCommit);
        response.put("serverTime", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        return ResponseEntity.ok(response);
    }
}

