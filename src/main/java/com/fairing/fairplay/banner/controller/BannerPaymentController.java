package com.fairing.fairplay.banner.controller;

import com.fairing.fairplay.banner.dto.BannerPaymentPageDto;
import com.fairing.fairplay.banner.entity.BannerApplication;
import com.fairing.fairplay.banner.entity.BannerPaymentStatus;
import com.fairing.fairplay.banner.repository.BannerApplicationRepository;
import com.fairing.fairplay.banner.service.BannerApplicationService;
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
import com.fairing.fairplay.user.entity.Users;
import com.fairing.fairplay.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/banners/payment")
@RequiredArgsConstructor
@Slf4j
public class BannerPaymentController {

    private final PaymentService paymentService;
    private final BannerApplicationRepository bannerApplicationRepository;
    private final PaymentStatusCodeRepository paymentStatusCodeRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;
    private final BannerApplicationService bannerApplicationService;

    // 배너 결제 요청 (인증된 사용자)
    @PostMapping("/request")
    @FunctionAuth("requestBannerPayment")
    public ResponseEntity<PaymentResponseDto> requestBannerPayment(
            @RequestBody PaymentRequestDto paymentRequestDto,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        
        paymentRequestDto.setPaymentTargetType("BANNER_APPLICATION");
        PaymentResponseDto savedPayment = paymentService.savePayment(paymentRequestDto, userDetails.getUserId());
        return ResponseEntity.ok(savedPayment);
    }

    // 배너 결제 완료 처리
    @PostMapping("/complete")
    @FunctionAuth("completeBannerPayment")
    public ResponseEntity<PaymentResponseDto> completeBannerPayment(
            @RequestBody PaymentRequestDto paymentRequestDto) {
        
        PaymentResponseDto completedPayment = paymentService.completePayment(paymentRequestDto);
        PaymentStatusCode paymentStatusCode = paymentStatusCodeRepository.findById(completedPayment.getPaymentStatusCodeId())
                .orElseThrow(() -> new CustomException(HttpStatus.NOT_FOUND, "해당 결제 상태 코드를 찾을 수 없습니다."));

        // 결제 완료 시 배너 신청의 결제 상태를 PAID로 변경
        if ("COMPLETED".equals(paymentStatusCode.getCode()) && completedPayment.getTargetId() != null) {
            try {
                updateBannerApplicationPaymentStatus(completedPayment.getTargetId());
                
                // 결제 완료 시 배너 슬롯 활성화 (승인과 결제가 분리된 경우)
                bannerApplicationService.activateBannerSlots(completedPayment.getTargetId());
                
                // 결제 완료 알림 발송 (광고 신청자에게)
                sendPaymentCompletionNotification(completedPayment.getTargetId());
                
                log.info("배너 결제 완료 처리, 슬롯 활성화 및 알림 발송 완료 - ApplicationId: {}, PaymentId: {}", 
                        completedPayment.getTargetId(), completedPayment.getMerchantUid());
                        
            } catch (Exception e) {
                log.error("배너 결제 완료 후 처리 실패 - ApplicationId: {}, 오류: {}", 
                        completedPayment.getTargetId(), e.getMessage());
            }
        }
        
        return ResponseEntity.ok(completedPayment);
    }

    // 배너 결제 페이지 정보 조회 (이메일 링크에서 접근 - 인증 불필요)
    @GetMapping("/payment-page/{applicationId}")
    public ResponseEntity<BannerPaymentPageDto> getBannerPaymentPage(@PathVariable Long applicationId) {
        BannerApplication application = bannerApplicationRepository.findById(applicationId)
                .orElseThrow(() -> new CustomException(HttpStatus.NOT_FOUND, "배너 신청 정보를 찾을 수 없습니다."));
        
        // 승인된 신청만 결제 가능
        if (!"APPROVED".equals(application.getStatusCode().getCode())) {
            throw new CustomException(HttpStatus.BAD_REQUEST, "승인되지 않은 배너 신청입니다.");
        }
        
        // 이미 결제 완료된 경우
        if (BannerPaymentStatus.PAID == application.getPaymentStatus()) {
            throw new CustomException(HttpStatus.BAD_REQUEST, "이미 결제가 완료된 배너입니다.");
        }
        
        // 사용자 정보 조회
        Users applicant = userRepository.findById(application.getApplicantId().getUserId())
                .orElseThrow(() -> new CustomException(HttpStatus.NOT_FOUND, "신청자 정보를 찾을 수 없습니다."));
        
        BannerPaymentPageDto paymentInfo = BannerPaymentPageDto.builder()
                .applicationId(application.getId())
                .title(application.getTitle())
                .bannerType(application.getBannerType().getName())
                .totalAmount(application.getTotalAmount())
                .applicantName(applicant.getName())
                .applicantEmail(applicant.getEmail())
                .paymentStatus(application.getPaymentStatus().name())
                .startDate(application.getStartDate())
                .endDate(application.getEndDate())
                .build();
        
        return ResponseEntity.ok(paymentInfo);
    }

    // 배너 결제 요청 (이메일 링크에서 접근 - 인증 불필요)
    @PostMapping("/request-from-email")
    public ResponseEntity<PaymentResponseDto> requestBannerPaymentFromEmail(
            @RequestBody PaymentRequestDto paymentRequestDto) {

        paymentRequestDto.setPaymentTargetType("BANNER_APPLICATION");
        PaymentResponseDto savedPayment = paymentService.savePaymentFromEmail(paymentRequestDto);
        return ResponseEntity.ok(savedPayment);
    }

    private void updateBannerApplicationPaymentStatus(Long applicationId) {
        BannerApplication application = bannerApplicationRepository.findById(applicationId)
                .orElseThrow(() -> new CustomException(HttpStatus.NOT_FOUND, "배너 신청 정보를 찾을 수 없습니다."));
        
        application.updatePaymentStatus(BannerPaymentStatus.PAID);
        bannerApplicationRepository.save(application);
        
        log.info("배너 신청 결제 상태 업데이트 완료 - ApplicationId: {}", applicationId);
    }

    private void sendPaymentCompletionNotification(Long applicationId) {
        try {
            BannerApplication application = bannerApplicationRepository.findById(applicationId)
                    .orElse(null);
            
            if (application != null) {
                Users applicant = userRepository.findById(application.getApplicantId().getUserId())
                        .orElse(null);
                
                if (applicant != null) {
                    NotificationRequestDto notificationDto = new NotificationRequestDto();
                    notificationDto.setUserId(applicant.getUserId());
                    notificationDto.setTypeCode("BANNER_PAYMENT_COMPLETED");
                    notificationDto.setMethodCode("WEB");
                    notificationDto.setTitle("배너 광고 결제 완료");
                    notificationDto.setMessage(String.format("'%s' 배너 광고 결제가 완료되었습니다.", application.getTitle()));
                    
                    notificationService.createNotification(notificationDto);
                }
            }
        } catch (Exception e) {
            log.error("배너 결제 완료 알림 발송 실패 - ApplicationId: {}, 오류: {}", applicationId, e.getMessage());
        }
    }
}