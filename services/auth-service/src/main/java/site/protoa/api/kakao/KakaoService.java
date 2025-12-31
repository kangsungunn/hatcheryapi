package site.protoa.api.kakao;

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
import site.protoa.api.kakao.dto.KakaoTokenResponse;
import site.protoa.api.kakao.dto.KakaoUserInfo;

@Service
public class KakaoService {

    private final RestTemplate restTemplate;

    @Value("${kakao.rest-api-key}")
    private String kakaoRestApiKey;

    @Value("${kakao.redirect-uri}")
    private String kakaoRedirectUri;

    @Value("${kakao.client-secret:}")
    private String kakaoClientSecret;

    @Autowired
    public KakaoService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    /**
     * 카카오 인가 URL 생성
     * 
     * @return 카카오 인가 URL
     */
    public String getAuthorizationUrl() {
        return UriComponentsBuilder.fromUriString("https://kauth.kakao.com/oauth/authorize")
                .queryParam("client_id", kakaoRestApiKey)
                .queryParam("redirect_uri", kakaoRedirectUri)
                .queryParam("response_type", "code")
                .toUriString();
    }

    /**
     * 인가 코드로 액세스 토큰 요청
     * 
     * @param code 인가 코드
     * @return KakaoTokenResponse
     */
    public KakaoTokenResponse getAccessToken(String code) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        formData.add("grant_type", "authorization_code");
        formData.add("client_id", kakaoRestApiKey);
        formData.add("redirect_uri", kakaoRedirectUri);
        formData.add("code", code);

        // Client Secret이 있으면 추가
        if (kakaoClientSecret != null && !kakaoClientSecret.isEmpty()) {
            formData.add("client_secret", kakaoClientSecret);
        }

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(formData, headers);
        ResponseEntity<KakaoTokenResponse> response = restTemplate.postForEntity(
                "https://kauth.kakao.com/oauth/token",
                request,
                KakaoTokenResponse.class
        );

        return response.getBody();
    }

    /**
     * 액세스 토큰으로 사용자 정보 요청
     * 
     * @param accessToken 액세스 토큰
     * @return KakaoUserInfo
     */
    public KakaoUserInfo getUserInfo(String accessToken) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);

        HttpEntity<Void> request = new HttpEntity<>(headers);
        ResponseEntity<KakaoUserInfo> response = restTemplate.exchange(
                "https://kapi.kakao.com/v2/user/me",
                HttpMethod.GET,
                request,
                KakaoUserInfo.class
        );

        return response.getBody();
    }
}
