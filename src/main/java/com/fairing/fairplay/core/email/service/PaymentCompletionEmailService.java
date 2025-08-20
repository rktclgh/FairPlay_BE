package com.fairing.fairplay.core.email.service;

import com.fairing.fairplay.admin.repository.EmailTemplatesRepository;
import com.fairing.fairplay.core.util.EmailSender;
import com.fairing.fairplay.payment.entity.Payment;
import com.fairing.fairplay.user.entity.Users;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Slf4j
@Service
public class PaymentCompletionEmailService extends AbstractEmailService {

    public PaymentCompletionEmailService(EmailSender emailSender, EmailTemplatesRepository emailTemplatesRepository) {
        super(emailSender, emailTemplatesRepository);
    }

    /**
     * 결제/예매 완료 이메일 전송
     *
     * @param payment       결제 정보
     * @param reservationId 예약 ID
     */
    @Transactional
    public void sendPaymentCompletionEmail(Payment payment, Long reservationId) {
        try {
            Users user = payment.getUser();
            send(user.getEmail(), payment, reservationId);
            log.info("결제 완료 이메일 발송 성공 - userId: {}, paymentId: {}", user.getUserId(), payment.getPaymentId());
        } catch (Exception e) {
            log.error("결제 완료 이메일 발송 실패 - paymentId: {}, error: {}", payment.getPaymentId(), e.getMessage());
            // 이메일 발송 실패해도 결제는 성공으로 처리
        }
    }

    @Override
    protected EmailContent createEmailContent(Object... params) {
        Payment payment = (Payment) params[0];
        Long reservationId = (Long) params[1];

        // 무료 티켓 여부 확인
        boolean isFreeTicket = payment.getAmount().compareTo(BigDecimal.ZERO) == 0;
        String actionType = isFreeTicket ? "예매" : "결제";

        // HTML 템플릿 로드 및 데이터 치환
        String html = buildPaymentCompletionEmailHtml(payment, reservationId, isFreeTicket, actionType);

        // 이메일 제목 생성
        String eventTitle = payment.getEvent() != null ? payment.getEvent().getTitleKr() : "이벤트";
        String subject = String.format("[FairPlay] %s %s 완료", eventTitle, actionType);

        return new EmailContent(subject, html, "logo", "/etc/logo.png");
    }

    /**
     * 결제/예매 완료 이메일 HTML 템플릿 생성
     */
    private String buildPaymentCompletionEmailHtml(Payment payment, Long reservationId, boolean isFreeTicket, String actionType) {
        try (InputStream is = new ClassPathResource("email/payment-completion.html").getInputStream()) {
            String template = new String(is.readAllBytes(), StandardCharsets.UTF_8);

            // 템플릿 데이터 준비
            String userName = payment.getUser().getName();
            String eventTitle = payment.getEvent() != null ? payment.getEvent().getTitleKr() : "이벤트";
            String amountText = isFreeTicket ? "무료" : payment.getAmount().toString();
            String paidDateTime = formatDateTime(payment.getPaidAt());
            String reservationIdText = reservationId != null ? reservationId.toString() : "처리중";
            String merchantUid = payment.getMerchantUid();

            // 이벤트 날짜 정보
            String eventPeriod = "";
            if (payment.getEvent() != null && payment.getEvent().getEventDetail() != null) {
                var detail = payment.getEvent().getEventDetail();
                if (detail.getStartDate() != null && detail.getEndDate() != null) {
                    eventPeriod = String.format("%s ~ %s", 
                        detail.getStartDate().format(DateTimeFormatter.ofPattern("yyyy.MM.dd")),
                        detail.getEndDate().format(DateTimeFormatter.ofPattern("yyyy.MM.dd"))
                    );
                }
            }

            // 템플릿 치환
            return String.format(template,
                userName,           // %s - 사용자 이름
                eventTitle,         // %s - 이벤트명
                actionType,         // %s - 결제/예매
                eventTitle,         // %s - 이벤트명 (정보 섹션)
                amountText,         // %s - 금액
                paidDateTime,       // %s - 결제/예매 일시
                eventPeriod,        // %s - 이벤트 기간
                reservationIdText,  // %s - 예약 번호
                merchantUid,        // %s - 주문 번호
                actionType,         // %s - 결제/예매 (버튼 텍스트)
                isFreeTicket ? "무료 예매" : "유료 결제" // %s - 티켓 타입
            );

        } catch (Exception e) {
            log.error("결제 완료 이메일 템플릿 로딩 실패: {}", e.getMessage());
            throw new RuntimeException("결제 완료 이메일 템플릿 로딩 실패", e);
        }
    }

    /**
     * 날짜/시간 포맷팅
     */
    private String formatDateTime(LocalDateTime dateTime) {
        if (dateTime == null) {
            return LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy년 MM월 dd일 HH:mm"));
        }
        return dateTime.format(DateTimeFormatter.ofPattern("yyyy년 MM월 dd일 HH:mm"));
    }
}