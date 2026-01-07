package com.kroaddy.api.repository;

import com.kroaddy.api.entity.LoginLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LoginLogRepository extends JpaRepository<LoginLog, Long> {

    /**
     * userId로 로그 조회 (최신순)
     */
    List<LoginLog> findByUserIdOrderByLoginTimeDesc(String userId);

    /**
     * provider로 로그 조회 (최신순)
     */
    List<LoginLog> findByProviderOrderByLoginTimeDesc(String provider);

    /**
     * 특정 사용자의 최근 로그인 로그 조회
     */
    List<LoginLog> findTop10ByUserIdOrderByLoginTimeDesc(String userId);
}
