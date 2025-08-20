package com.fairing.fairplay.booth.service;

import com.fairing.fairplay.admin.entity.AccountLevel;
import com.fairing.fairplay.admin.repository.AccountLevelRepository;
import com.fairing.fairplay.booth.dto.BoothCancelPageDto;
import com.fairing.fairplay.booth.dto.BoothCancelRequestDto;
import com.fairing.fairplay.booth.entity.Booth;
import com.fairing.fairplay.booth.entity.BoothApplication;
import com.fairing.fairplay.booth.entity.BoothPaymentStatusCode;
import com.fairing.fairplay.booth.repository.BoothApplicationRepository;
import com.fairing.fairplay.booth.repository.BoothApplicationStatusCodeRepository;
import com.fairing.fairplay.booth.repository.BoothPaymentStatusCodeRepository;
import com.fairing.fairplay.booth.repository.BoothRepository;
import com.fairing.fairplay.common.exception.CustomException;
import com.fairing.fairplay.core.email.service.BoothEmailService;
import com.fairing.fairplay.file.entity.File;
import com.fairing.fairplay.file.repository.FileRepository;
import com.fairing.fairplay.file.service.FileService;
import com.fairing.fairplay.notification.dto.NotificationRequestDto;
import com.fairing.fairplay.notification.service.NotificationService;
import com.fairing.fairplay.payment.dto.PaymentRequestDto;
import com.fairing.fairplay.payment.entity.Payment;
import com.fairing.fairplay.payment.repository.PaymentRepository;
import com.fairing.fairplay.payment.service.RefundService;
import com.fairing.fairplay.user.entity.BoothAdmin;
import com.fairing.fairplay.user.entity.EventAdmin;
import com.fairing.fairplay.user.entity.Users;
import com.fairing.fairplay.user.repository.BoothAdminRepository;
import com.fairing.fairplay.user.repository.EventAdminRepository;
import com.fairing.fairplay.user.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;


@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class BoothCancelService {

    private final BoothApplicationRepository boothApplicationRepository;
    private final BoothApplicationStatusCodeRepository statusCodeRepository;
    private final BoothPaymentStatusCodeRepository paymentCodeRepository;
    private final BoothRepository boothRepository;
    private final UserRepository userRepository;
    private final BoothAdminRepository boothAdminRepository;
    private final AccountLevelRepository accountLevelRepository;
    private final EventAdminRepository eventAdminRepository;
    private final FileRepository fileRepository;
    private final FileService fileService;
    private final BoothEmailService boothEmailService;
    private final NotificationService notificationService;
    private final PaymentRepository paymentRepository;
    private final RefundService refundService;

    @Transactional(readOnly = true)
    public BoothCancelPageDto getBoothCancelInfo(Long applicationId) {
        BoothApplication application = boothApplicationRepository.findById(applicationId)
                .orElseThrow(() -> new CustomException(HttpStatus.NOT_FOUND, "부스 신청 정보를 찾을 수 없습니다."));

        // 승인된 부스만 취소 가능
        boolean canCancel = "APPROVED".equals(application.getBoothApplicationStatusCode().getCode()) &&
                           !"CANCELLED".equals(application.getBoothPaymentStatusCode().getCode());

        return BoothCancelPageDto.builder()
                .applicationId(application.getId())
                .eventTitle(application.getEvent().getTitleKr())
                .boothTitle(application.getBoothTitle())
                .boothTypeName(application.getBoothType().getName())
                .boothTypeSize(application.getBoothType().getSize())
                .price(application.getBoothType().getPrice())
                .managerName(application.getManagerName())
                .contactEmail(application.getContactEmail())
                .applicationStatus(application.getBoothApplicationStatusCode().getCode())
                .paymentStatus(application.getBoothPaymentStatusCode().getCode())
                .canCancel(canCancel)
                .build();
    }

    @Transactional
    public void requestBoothCancel(Long applicationId, BoothCancelRequestDto cancelRequest) {
        BoothApplication application = boothApplicationRepository.findById(applicationId)
                .orElseThrow(() -> new CustomException(HttpStatus.NOT_FOUND, "부스 신청 정보를 찾을 수 없습니다."));

        // 이메일 검증
        if (!application.getContactEmail().equalsIgnoreCase(cancelRequest.getContactEmail())) {
            throw new CustomException(HttpStatus.BAD_REQUEST, "등록된 연락처 이메일과 일치하지 않습니다.");
        }

        // 취소 가능 상태 확인
        if (!"APPROVED".equals(application.getBoothApplicationStatusCode().getCode())) {
            throw new CustomException(HttpStatus.BAD_REQUEST, "승인된 부스만 취소할 수 있습니다.");
        }

        if ("CANCELLED".equals(application.getBoothPaymentStatusCode().getCode())) {
            throw new CustomException(HttpStatus.BAD_REQUEST, "이미 취소된 부스입니다.");
        }

        try {
            // 1. 부스 결제 상태를 CANCELLED로 변경
            BoothPaymentStatusCode cancelledPaymentStatus = paymentCodeRepository.findByCode("CANCELLED")
                    .orElseThrow(() -> new EntityNotFoundException("CANCELLED 결제 상태 코드를 찾을 수 없습니다."));

            application.setBoothPaymentStatusCode(cancelledPaymentStatus);
            application.setAdminComment("사용자 요청에 의한 취소: " + cancelRequest.getCancelReason());
            application.setStatusUpdatedAt(LocalDateTime.now());
            
            boothApplicationRepository.save(application);

            // 2. 결제 정보 처리 (환불 요청)
            handlePaymentForCancellation(application);

            // 3. 부스 관리자 계정 및 부스 삭제
            deleteBoothAndAdmin(application);

            // 4. 부스 관련 파일 삭제
            deleteBoothFiles(application);

            // 5. 취소 확인 이메일 발송
            sendCancelConfirmationEmail(application, cancelRequest.getCancelReason());

            // 6. 관리자에게 취소 알림
            sendCancelNotificationToAdmin(application);

            log.info("부스 취소 처리 완료 - ApplicationId: {}, 취소 사유: {}", 
                    applicationId, cancelRequest.getCancelReason());

        } catch (Exception e) {
            log.error("부스 취소 처리 중 오류 발생 - ApplicationId: {}, 오류: {}", applicationId, e.getMessage());
            throw new CustomException(HttpStatus.INTERNAL_SERVER_ERROR, "부스 취소 처리 중 오류가 발생했습니다.");
        }
    }

    private void deleteBoothAndAdmin(BoothApplication application) {
        try {
            // 부스 관리자 계정 조회
            Users adminUser = userRepository.findByEmail(application.getBoothEmail()).orElse(null);
            if (adminUser != null) {
                // 1. 먼저 부스에서 booth_admin_id 참조를 null로 설정하고 삭제 처리
                List<Booth> booths = boothRepository.findByEventAndIsDeletedFalse(application.getEvent());
                booths.stream()
                        .filter(booth -> booth.getBoothTitle().equals(application.getBoothTitle()))
                        .forEach(booth -> {
                            // 외래 키 참조를 제거
                            booth.setBoothAdmin(null);
                            booth.setIsDeleted(true);
                            boothRepository.save(booth);
                            log.info("부스 삭제 완료 - BoothId: {}", booth.getId());
                        });

                // 2. 그 다음 BoothAdmin 엔티티 삭제
                BoothAdmin boothAdmin = boothAdminRepository.findByUser(adminUser).orElse(null);
                if (boothAdmin != null) {
                    boothAdminRepository.delete(boothAdmin);
                    log.info("BoothAdmin 삭제 완료 - UserId: {}", adminUser.getUserId());
                }

                // 3. AccountLevel 엔티티 삭제 (Users에 대한 외래 키 참조 제거)
                AccountLevel accountLevel = accountLevelRepository.findByUserId(adminUser.getUserId());
                if (accountLevel != null) {
                    accountLevelRepository.delete(accountLevel);
                    log.info("AccountLevel 삭제 완료 - UserId: {}", adminUser.getUserId());
                }

                // 4. Users 엔티티 소프트 삭제 (결제 기록 보존을 위해 물리적 삭제 대신 소프트 삭제)
                adminUser.setDeletedAt(LocalDateTime.now());
                userRepository.save(adminUser);
                log.info("부스 관리자 계정 소프트 삭제 완료 - Email: {}", application.getBoothEmail());
            }

        } catch (Exception e) {
            log.error("부스 및 관리자 계정 삭제 중 오류 발생 - ApplicationId: {}, 오류: {}", 
                    application.getId(), e.getMessage());
            // 외래 키 제약 조건 오류인 경우 더 자세한 로그
            if (e.getMessage().contains("foreign key constraint")) {
                log.error("외래 키 제약 조건 위반: 부스 테이블에서 booth_admin_id 참조가 남아있습니다. 부스 삭제를 먼저 확인해주세요.");
            }
        }
    }

    private void deleteBoothFiles(BoothApplication application) {
        try {
            // 부스 신청 관련 파일 삭제
            List<File> filesToDelete = fileRepository.findByTargetTypeAndTargetId("BOOTH_APPLICATION", application.getId());
            for (File file : filesToDelete) {
                fileService.deleteFile(file.getId());
            }

            // 부스 관련 파일 삭제 (부스가 생성된 경우)
            List<Booth> booths = boothRepository.findByEventAndIsDeletedFalse(application.getEvent());
            booths.stream()
                    .filter(booth -> booth.getBoothTitle().equals(application.getBoothTitle()))
                    .forEach(booth -> {
                        List<File> boothFiles = fileRepository.findByTargetTypeAndTargetId("BOOTH", booth.getId());
                        for (File file : boothFiles) {
                            fileService.deleteFile(file.getId());
                        }
                    });

            log.info("부스 관련 파일 삭제 완료 - ApplicationId: {}", application.getId());
        } catch (Exception e) {
            log.error("부스 파일 삭제 중 오류 발생 - ApplicationId: {}, 오류: {}", 
                    application.getId(), e.getMessage());
        }
    }

    private void sendCancelConfirmationEmail(BoothApplication application, String cancelReason) {
        try {
            boothEmailService.sendCancelConfirmationEmail(
                    application.getContactEmail(),
                    application.getEvent().getTitleKr(),
                    application.getBoothTitle(),
                    cancelReason
            );
            log.info("취소 확인 이메일 발송 완료 - ApplicationId: {}", application.getId());
        } catch (Exception e) {
            log.error("취소 확인 이메일 발송 실패 - ApplicationId: {}, 오류: {}", 
                    application.getId(), e.getMessage());
        }
    }

    private void handlePaymentForCancellation(BoothApplication application) {
        try {
            // 부스 신청 ID로 결제 정보 조회
            List<Payment> payments = paymentRepository.findByPaymentTargetType_PaymentTargetCodeAndTargetId(
                    "BOOTH_APPLICATION", application.getId());
            
            for (Payment payment : payments) {
                if ("PAID".equals(payment.getPaymentStatusCode().getCode())) {
                    // 결제 완료된 건에 대해 자동 환불 요청 생성
                    try {
                        PaymentRequestDto refundRequest = new PaymentRequestDto();
                        refundRequest.setRefundRequestAmount(payment.getAmount());
                        refundRequest.setReason("부스 취소에 따른 자동 환불");
                        
                        refundService.requestRefund(payment.getPaymentId(), refundRequest, payment.getUser().getUserId());
                        log.info("부스 취소로 인한 환불 요청 생성 완료 - PaymentId: {}", payment.getPaymentId());
                    } catch (Exception e) {
                        log.error("환불 요청 생성 실패 - PaymentId: {}, 오류: {}", 
                                payment.getPaymentId(), e.getMessage());
                    }
                }
            }
        } catch (Exception e) {
            log.error("부스 취소 시 결제 정보 처리 실패 - ApplicationId: {}, 오류: {}", 
                    application.getId(), e.getMessage());
        }
    }

    private void sendCancelNotificationToAdmin(BoothApplication application) {
        try {
            // 해당 이벤트의 관리자에게 알림 발송
            EventAdmin eventAdmin = eventAdminRepository.findByEvent(application.getEvent());
            
            if (eventAdmin == null) {
                log.warn("이벤트 관리자를 찾을 수 없어 부스 취소 알림을 발송할 수 없습니다. - EventId: {}", 
                        application.getEvent().getEventId());
                return;
            }

            // 관리자가 활성화되지 않은 경우 알림 발송하지 않음
            if (!eventAdmin.getActive() || !eventAdmin.getVerified()) {
                log.warn("이벤트 관리자가 비활성 상태입니다. 부스 취소 알림을 발송하지 않습니다. - EventId: {}, AdminUserId: {}", 
                        application.getEvent().getEventId(), eventAdmin.getUser().getUserId());
                return;
            }

            NotificationRequestDto notificationDto = new NotificationRequestDto();
            notificationDto.setUserId(eventAdmin.getUser().getUserId());
            notificationDto.setTypeCode("BOOTH_CANCELLED");
            notificationDto.setMethodCode("WEB");
            notificationDto.setTitle("부스 취소 요청");
            notificationDto.setMessage(String.format("'%s' 이벤트의 '%s' 부스가 사용자 요청에 의해 취소되었습니다.", 
                    application.getEvent().getTitleKr(), application.getBoothTitle()));

            notificationService.createNotification(notificationDto);
            log.info("관리자 취소 알림 발송 완료 - ApplicationId: {}, AdminUserId: {}", 
                    application.getId(), eventAdmin.getUser().getUserId());
        } catch (Exception e) {
            log.error("관리자 취소 알림 발송 실패 - ApplicationId: {}, 오류: {}", 
                    application.getId(), e.getMessage());
        }
    }
}