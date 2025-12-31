package com.kroaddy.api.naver;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;
import com.kroaddy.api.naver.dto.NaverTokenResponse;
import com.kroaddy.api.naver.dto.NaverUserInfo;

import java.util.UUID;

@Service
public class NaverService {

    private final RestTemplate restTemplate;

    @Value("${naver.client-id}")
    private String naverClientId;

    @Value("${naver.client-secret}")
    private String naverClientSecret;

    @Value("${naver.redirect-uri}")
    private String naverRedirectUri;

    @Autowired
    public NaverService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    /**
     * 네이버 인가 URL 생성
     * 
     * @return 네이버 인가 URL
     */
    public String getAuthorizationUrl() {
        String state = UUID.randomUUID().toString();
        return UriComponentsBuilder.fromUriString("https://nid.naver.com/oauth2.0/authorize")
                .queryParam("response_type", "code")
                .queryParam("client_id", naverClientId)
                .queryParam("redirect_uri", naverRedirectUri)
                .queryParam("state", state)
                .toUriString();
    }

    /**
     * 인가 코드로 액세스 토큰 요청
     * 
     * @param code  인가 코드
     * @param state 상태 값
     * @return NaverTokenResponse
     */
    public NaverTokenResponse getAccessToken(String code, String state) {
        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        formData.add("grant_type", "authorization_code");
        formData.add("client_id", naverClientId);
        formData.add("client_secret", naverClientSecret);
        formData.add("code", code);
        formData.add("state", state);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(formData, headers);

        ResponseEntity<NaverTokenResponse> response = restTemplate.exchange(
                "https://nid.naver.com/oauth2.0/token",
                HttpMethod.POST,
                request,
                NaverTokenResponse.class
        );

        return response.getBody();
    }

    /**
     * 액세스 토큰으로 사용자 정보 요청
     * 
     * @param accessToken 액세스 토큰
     * @return NaverUserInfo
     */
    public NaverUserInfo getUserInfo(String accessToken) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);

        HttpEntity<String> request = new HttpEntity<>(headers);

        ResponseEntity<NaverUserInfo> response = restTemplate.exchange(
                "https://openapi.naver.com/v1/nid/me",
                HttpMethod.GET,
                request,
                NaverUserInfo.class
        );

        return response.getBody();
    }
}

