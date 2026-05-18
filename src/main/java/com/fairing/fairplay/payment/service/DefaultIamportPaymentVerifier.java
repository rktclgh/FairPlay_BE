package com.fairing.fairplay.payment.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.net.ssl.HttpsURLConnection;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.math.BigDecimal;
import java.net.URL;
import java.util.Map;

@Component
public class DefaultIamportPaymentVerifier implements IamportPaymentVerifier {

    @Value("${iamport.api-key}")
    private String iamportApiKey;

    @Value("${iamport.secret-key}")
    private String iamportSecretKey;

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

    private String getIamportAccessToken() throws IOException {
        URL url = new URL("https://api.iamport.kr/users/getToken");
        HttpsURLConnection conn = (HttpsURLConnection) url.openConnection();

        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setRequestProperty("Accept", "application/json");
        conn.setDoOutput(true);

        ObjectMapper mapper = new ObjectMapper();
        ObjectNode objectNode = mapper.createObjectNode();
        objectNode.put("imp_key", iamportApiKey);
        objectNode.put("imp_secret", iamportSecretKey);

        try (BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(conn.getOutputStream()))) {
            bw.write(mapper.writeValueAsString(objectNode));
            bw.flush();
        }

        try (BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
            String jsonLine = br.readLine();
            Map<String, Object> topLevelMap = mapper.readValue(jsonLine, Map.class);
            Map<String, Object> responseMap = (Map<String, Object>) topLevelMap.get("response");
            return responseMap.get("access_token").toString();
        } finally {
            conn.disconnect();
        }
    }

    private Map<String, Object> getPaymentInfoFromIamport(String impUid, String accessToken) throws IOException {
        URL url = new URL("https://api.iamport.kr/payments/" + impUid);
        HttpsURLConnection conn = (HttpsURLConnection) url.openConnection();

        conn.setRequestMethod("GET");
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setRequestProperty("Accept", "application/json");
        conn.setRequestProperty("Authorization", "Bearer " + accessToken);

        try (BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
            String jsonLine = br.readLine();

            ObjectMapper objectMapper = new ObjectMapper();
            Map<String, Object> topLevelMap = objectMapper.readValue(jsonLine, Map.class);

            Integer code = (Integer) topLevelMap.get("code");
            if (code == null || code != 0) {
                throw new IllegalStateException("아임포트 API 응답 오류: " + topLevelMap.get("message"));
            }

            return (Map<String, Object>) topLevelMap.get("response");
        } finally {
            conn.disconnect();
        }
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
        if (value instanceof Number number) {
            return BigDecimal.valueOf(number.doubleValue());
        }
        return new BigDecimal(value.toString());
    }
}
