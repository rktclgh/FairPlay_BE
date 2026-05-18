package com.fairing.fairplay.payment.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class DefaultIamportPaymentVerifierTest {

    @Test
    void iamportRestTemplateHasConnectAndReadTimeouts() {
        RestTemplate restTemplate = DefaultIamportPaymentVerifier.createIamportRestTemplate();

        assertThat(restTemplate.getRequestFactory()).isInstanceOf(SimpleClientHttpRequestFactory.class);
        SimpleClientHttpRequestFactory requestFactory =
                (SimpleClientHttpRequestFactory) restTemplate.getRequestFactory();

        assertThat(ReflectionTestUtils.getField(requestFactory, "connectTimeout"))
                .isEqualTo((int) DefaultIamportPaymentVerifier.IAMPORT_CONNECT_TIMEOUT.toMillis());
        assertThat(ReflectionTestUtils.getField(requestFactory, "readTimeout"))
                .isEqualTo((int) DefaultIamportPaymentVerifier.IAMPORT_READ_TIMEOUT.toMillis());
    }

    @Test
    void findPaymentUsesTokenThenPaymentLookupAndParsesResponse() {
        RestTemplate restTemplate = DefaultIamportPaymentVerifier.createIamportRestTemplate();
        MockRestServiceServer server = MockRestServiceServer.bindTo(restTemplate).build();
        DefaultIamportPaymentVerifier verifier = new DefaultIamportPaymentVerifier(restTemplate, new ObjectMapper());
        ReflectionTestUtils.setField(verifier, "iamportApiKey", "api-key");
        ReflectionTestUtils.setField(verifier, "iamportSecretKey", "secret-key");
        ReflectionTestUtils.setField(verifier, "iamportBaseUrl", "https://api.iamport.kr");

        server.expect(requestTo("https://api.iamport.kr/users/getToken"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess("""
                        {"code":0,"response":{"access_token":"access-token"}}
                        """, MediaType.APPLICATION_JSON));
        server.expect(requestTo("https://api.iamport.kr/payments/imp-valid"))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header("Authorization", "Bearer access-token"))
                .andRespond(withSuccess("""
                        {"code":0,"response":{"imp_uid":"imp-valid","merchant_uid":"merchant-1","status":"paid","amount":12000}}
                        """, MediaType.APPLICATION_JSON));

        IamportPaymentInfo paymentInfo = verifier.findPayment("imp-valid");

        assertThat(paymentInfo.impUid()).isEqualTo("imp-valid");
        assertThat(paymentInfo.merchantUid()).isEqualTo("merchant-1");
        assertThat(paymentInfo.status()).isEqualTo("paid");
        assertThat(paymentInfo.amount()).isEqualByComparingTo(BigDecimal.valueOf(12000));
        server.verify();
    }
}
