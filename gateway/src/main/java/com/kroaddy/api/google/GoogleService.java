package com.kroaddy.api.google;

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
import com.kroaddy.api.google.dto.GoogleTokenResponse;
import com.kroaddy.api.google.dto.GoogleUserInfo;

@Service
public class GoogleService {

    private final RestTemplate restTemplate;

    @Value("${google.client-id:}")
    private String googleClientId;

    @Value("${google.client-secret:}")
    private String googleClientSecret;

    @Value("${google.redirect-uri:}")
    private String googleRedirectUri;

    @Autowired
    public GoogleService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    /**
     * 구글 인가 URL 생성
     * 
     * @return 구글 인가 URL
     */
    public String getAuthorizationUrl() {
        if (googleClientId == null || googleClientId.isEmpty()) {
            throw new IllegalStateException("GOOGLE_CLIENT_ID가 설정되지 않았습니다. 환경 변수를 확인하세요.");
        }
        if (googleRedirectUri == null || googleRedirectUri.isEmpty()) {
            throw new IllegalStateException("GOOGLE_REDIRECT_URI가 설정되지 않았습니다. 환경 변수를 확인하세요.");
        }
        return UriComponentsBuilder.fromUriString("https://accounts.google.com/o/oauth2/v2/auth")
                .queryParam("client_id", googleClientId)
                .queryParam("redirect_uri", googleRedirectUri)
                .queryParam("response_type", "code")
                .queryParam("scope", "openid email profile")
                .queryParam("access_type", "offline")
                .queryParam("prompt", "consent")
                .toUriString();
    }

    /**
     * 인가 코드로 액세스 토큰 요청
     * 
     * @param code 인가 코드
     * @return GoogleTokenResponse
     */
    public GoogleTokenResponse getAccessToken(String code) {
        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        formData.add("code", code);
        formData.add("client_id", googleClientId);
        formData.add("client_secret", googleClientSecret);
        formData.add("redirect_uri", googleRedirectUri);
        formData.add("grant_type", "authorization_code");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(formData, headers);

        ResponseEntity<GoogleTokenResponse> response = restTemplate.exchange(
                "https://oauth2.googleapis.com/token",
                HttpMethod.POST,
                request,
                GoogleTokenResponse.class
        );

        return response.getBody();
    }

    /**
     * 액세스 토큰으로 사용자 정보 요청
     * 
     * @param accessToken 액세스 토큰
     * @return GoogleUserInfo
     */
    public GoogleUserInfo getUserInfo(String accessToken) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);

        HttpEntity<String> request = new HttpEntity<>(headers);

        ResponseEntity<GoogleUserInfo> response = restTemplate.exchange(
                "https://www.googleapis.com/oauth2/v2/userinfo",
                HttpMethod.GET,
                request,
                GoogleUserInfo.class
        );

        return response.getBody();
    }
}

