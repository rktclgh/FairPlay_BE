package com.fairing.fairplay.booth.controller;

import com.fairing.fairplay.booth.dto.BoothPaymentPageDto;
import com.fairing.fairplay.booth.dto.BoothPaymentStatusUpdateDto;
import com.fairing.fairplay.booth.entity.Booth;
import com.fairing.fairplay.booth.entity.BoothApplication;
import com.fairing.fairplay.booth.repository.BoothApplicationRepository;
import com.fairing.fairplay.booth.repository.BoothRepository;
import com.fairing.fairplay.booth.service.BoothApplicationService;
import com.fairing.fairplay.user.entity.Users;
import com.fairing.fairplay.user.repository.UserRepository;
import com.fairing.fairplay.common.exception.CustomException;
import com.fairing.fairplay.core.etc.FunctionAuth;
import com.fairing.fairplay.core.security.CustomUserDetails;
import com.fairing.fairplay.notification.dto.NotificationRequestDto;
import com.fairing.fairplay.notification.service.NotificationService;
import com.fairing.fairplay.payment.dto.PaymentRequestDto;
import com.fairing.fairplay.payment.dto.PaymentResponseDto;
import com.fairing.fairplay.payment.entity.PaymentStatusCode;
import com.fairing.fairplay.payment.repository.PaymentStatusCodeRepository;
import com.fairing.fairplay.payment.service.PaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/booths/payment")
@RequiredArgsConstructor
@Slf4j
public class BoothPaymentController {

    private final PaymentService paymentService;
    private final BoothApplicationService boothApplicationService;
    private final NotificationService notificationService;
    private final PaymentStatusCodeRepository paymentStatusCodeRepository;
    private final BoothRepository boothRepository;
    private final BoothApplicationRepository boothApplicationRepository;
    private final UserRepository userRepository;

    // 부스 결제 요청
    @PostMapping("/request")
    @FunctionAuth("requestBoothPayment")
    public ResponseEntity<PaymentResponseDto> requestBoothPayment(
            @RequestBody PaymentRequestDto paymentRequestDto,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        
        paymentRequestDto.setPaymentTargetType("BOOTH_APPLICATION");
        PaymentResponseDto savedPayment = paymentService.savePayment(paymentRequestDto, userDetails.getUserId());
        return ResponseEntity.ok(savedPayment);
    }

    // 부스 결제 완료 처리
    @PostMapping("/complete")
    @FunctionAuth("completeBoothPayment")
    public ResponseEntity<PaymentResponseDto> completeBoothPayment(
            @RequestBody PaymentRequestDto paymentRequestDto) {
        
        PaymentResponseDto completedPayment = paymentService.completePayment(paymentRequestDto);
        PaymentStatusCode paymentStatusCode = paymentStatusCodeRepository.findById(completedPayment.getPaymentStatusCodeId())
                .orElseThrow(() -> new CustomException(HttpStatus.NOT_FOUND, "해당 결제 상태 코드를 찾을 수 없습니다."));

        // 결제 완료 시 부스 신청의 결제 상태를 PAID로 변경
        if ("COMPLETED".equals(paymentStatusCode.getCode()) && completedPayment.getTargetId() != null) {
            try {
                BoothPaymentStatusUpdateDto statusUpdateDto = new BoothPaymentStatusUpdateDto();
                statusUpdateDto.setPaymentStatusCode("PAID");
                statusUpdateDto.setAdminComment("결제 완료");
                
                boothApplicationService.updatePaymentStatus(completedPayment.getTargetId(), statusUpdateDto);
                
                // 결제 완료 알림 발송 (부스 관리자에게)
                NotificationRequestDto notificationDto = new NotificationRequestDto();
                notificationDto.setTypeCode("BOOTH_PAYMENT_COMPLETED");
                notificationDto.setMethodCode("WEB");
                notificationDto.setTitle("부스 결제 완료");
                notificationDto.setMessage(String.format("부스 신청 ID: %d - 결제가 완료되었습니다.", completedPayment.getTargetId()));

                // 부스 신청에서 사용자 정보 조회
                BoothApplication boothApplication = boothApplicationRepository.findById(completedPayment.getTargetId())
                        .orElse(null);
                
                if (boothApplication != null) {
                    // 부스 관리자 계정의 userId를 찾아야 함 - 부스 신청의 boothEmail로 Users 테이블에서 조회
                    Users boothUser = userRepository.findByEmail(boothApplication.getBoothEmail())
                            .orElse(null);
                    
                    if (boothUser != null) {
                        notificationDto.setUserId(boothUser.getUserId());
                        notificationService.createNotification(notificationDto);
                    }
                }
                
                log.info("부스 결제 완료 처리 및 알림 발송 완료 - ApplicationId: {}, PaymentId: {}", 
                        completedPayment.getTargetId(), completedPayment.getMerchantUid());
                        
            } catch (Exception e) {
                log.error("부스 결제 완료 후 상태 업데이트 실패 - ApplicationId: {}, 오류: {}", 
                        completedPayment.getTargetId(), e.getMessage());
                // 결제는 성공했지만 상태 업데이트 실패 시에도 결제 결과는 반환
            }
        }
        
        return ResponseEntity.ok(completedPayment);
    }

    // 부스 결제 페이지 정보 조회 (이메일 링크에서 접근 - 인증 불필요)
    @GetMapping("/payment-page/{applicationId}")
    public ResponseEntity<BoothPaymentPageDto> getBoothPaymentPage(@PathVariable Long applicationId) {
        BoothPaymentPageDto paymentInfo = boothApplicationService.getBoothPaymentInfo(applicationId);
        return ResponseEntity.ok(paymentInfo);
    }

    // 부스 결제 요청 (이메일 링크에서 접근 - 인증 불필요)
    @PostMapping("/request-from-email")
    public ResponseEntity<PaymentResponseDto> requestBoothPaymentFromEmail(
            @RequestBody PaymentRequestDto paymentRequestDto) {

        paymentRequestDto.setPaymentTargetType("BOOTH_APPLICATION");
        // 이메일에서 접근하는 경우 userId는 부스 신청자 정보에서 가져와야 함
        PaymentResponseDto savedPayment = paymentService.savePaymentFromEmail(paymentRequestDto);
        return ResponseEntity.ok(savedPayment);
    }
}