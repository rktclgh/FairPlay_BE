package com.fairing.fairplay.payment.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.Duration;
import java.util.List;
import java.util.Map;

@Component
public class DefaultIamportPaymentVerifier implements IamportPaymentVerifier {

    static final Duration IAMPORT_CONNECT_TIMEOUT = Duration.ofSeconds(5);
    static final Duration IAMPORT_READ_TIMEOUT = Duration.ofSeconds(10);

    private static final String IAMPORT_TOKEN_PATH = "/users/getToken";
    private static final String IAMPORT_PAYMENT_PATH = "/payments/";
    private static final ParameterizedTypeReference<Map<String, Object>> MAP_RESPONSE_TYPE =
            new ParameterizedTypeReference<>() {
            };

    @Value("${iamport.api-key}")
    private String iamportApiKey;

    @Value("${iamport.secret-key}")
    private String iamportSecretKey;

    @Value("${iamport.base-url:https://api.iamport.kr}")
    private String iamportBaseUrl = "https://api.iamport.kr";

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    public DefaultIamportPaymentVerifier() {
        this(createIamportRestTemplate(), new ObjectMapper());
    }

    DefaultIamportPaymentVerifier(RestTemplate restTemplate, ObjectMapper objectMapper) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
    }

    static RestTemplate createIamportRestTemplate() {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(IAMPORT_CONNECT_TIMEOUT);
        requestFactory.setReadTimeout(IAMPORT_READ_TIMEOUT);
        return new RestTemplate(requestFactory);
    }

    @Override
    public IamportPaymentInfo findPayment(String impUid) {
        try {
            String accessToken = getIamportAccessToken();
            Map<String, Object> paymentInfo = getPaymentInfoFromIamport(impUid, accessToken);
            return new IamportPaymentInfo(
                    asString(paymentInfo.get("imp_uid")),
                    asString(paymentInfo.get("merchant_uid")),
                    asString(paymentInfo.get("status")),
                    toBigDecimal(paymentInfo.get("amount"))
            );
        } catch (Exception e) {
            throw new IllegalStateException("아임포트 결제 조회 실패: " + e.getMessage(), e);
        }
    }

    private String getIamportAccessToken() {
        Map<String, String> requestBody = Map.of(
                "imp_key", iamportApiKey,
                "imp_secret", iamportSecretKey
        );
        HttpEntity<Map<String, String>> request = new HttpEntity<>(requestBody, jsonHeaders());

        ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                iamportBaseUrl + IAMPORT_TOKEN_PATH,
                HttpMethod.POST,
                request,
                MAP_RESPONSE_TYPE
        );

        Map<String, Object> responseMap = responseMap(response.getBody());
        return responseMap.get("access_token").toString();
    }

    private Map<String, Object> getPaymentInfoFromIamport(String impUid, String accessToken) {
        HttpHeaders headers = jsonHeaders();
        headers.setBearerAuth(accessToken);
        HttpEntity<Void> request = new HttpEntity<>(headers);

        ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                iamportBaseUrl + IAMPORT_PAYMENT_PATH + impUid,
                HttpMethod.GET,
                request,
                MAP_RESPONSE_TYPE
        );

        Map<String, Object> topLevelMap = response.getBody();
        Integer code = objectMapper.convertValue(topLevelMap.get("code"), Integer.class);
        if (code == null || code != 0) {
            throw new IllegalStateException("아임포트 API 응답 오류: " + topLevelMap.get("message"));
        }

        return responseMap(topLevelMap);
    }

    private HttpHeaders jsonHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));
        return headers;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> responseMap(Map<String, Object> topLevelMap) {
        return objectMapper.convertValue(topLevelMap.get("response"), Map.class);
    }

    private String asString(Object value) {
        return value != null ? value.toString() : null;
    }

    private BigDecimal toBigDecimal(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof BigDecimal bigDecimal) {
            return bigDecimal;
        }
        if (value instanceof BigInteger bigInteger) {
            return new BigDecimal(bigInteger);
        }
        if (value instanceof Number number) {
            return new BigDecimal(number.toString());
        }
        return new BigDecimal(value.toString());
    }
}
