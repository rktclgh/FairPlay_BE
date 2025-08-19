package com.fairing.fairplay.payment.service;

import com.fairing.fairplay.core.security.CustomUserDetails;
import com.fairing.fairplay.payment.dto.PaymentAdminDto;
import com.fairing.fairplay.payment.dto.PaymentSearchCriteria;
import com.fairing.fairplay.payment.entity.Payment;
import com.fairing.fairplay.payment.repository.PaymentAdminRepository;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PaymentAdminService {

    private final PaymentAdminRepository paymentAdminRepository;

    /**
     * 결제 내역 목록 조회 (권한별 필터링 적용)
     */
    public Page<PaymentAdminDto> getPaymentList(PaymentSearchCriteria criteria, CustomUserDetails userDetails) {
        validateUserPermission(userDetails);
        
        // 페이징 및 정렬 설정
        Sort sort = createSort(criteria.getSort(), criteria.getDirection());
        Pageable pageable = PageRequest.of(criteria.getPage(), criteria.getSize(), sort);
        
        // 권한에 따른 필터링
        Long managerId = getManagerIdForFiltering(userDetails);
        
        // QueryDSL을 통한 동적 쿼리 실행
        Page<Payment> paymentPage = paymentAdminRepository.findPaymentsWithCriteria(criteria, managerId, pageable);
        
        // DTO 변환
        return paymentPage.map(PaymentAdminDto::fromEntity);
    }

    /**
     * 결제 상세 정보 조회 (권한 검증 포함)
     */
    public PaymentAdminDto getPaymentDetail(Long paymentId, CustomUserDetails userDetails) {
        validateUserPermission(userDetails);
        
        Long managerId = getManagerIdForFiltering(userDetails);
        Payment payment = paymentAdminRepository.findPaymentWithPermissionCheck(paymentId, managerId)
                .orElseThrow(() -> new IllegalArgumentException("결제 정보를 찾을 수 없거나 접근 권한이 없습니다."));
        
        return PaymentAdminDto.fromEntity(payment);
    }

    /**
     * 결제 내역 엑셀 다운로드
     */
    public byte[] exportPaymentExcel(PaymentSearchCriteria criteria, CustomUserDetails userDetails) {
        validateUserPermission(userDetails);
        
        Long managerId = getManagerIdForFiltering(userDetails);
        
        // 엑셀 다운로드용으로는 페이징 제한을 늘림 (최대 10,000건)
        criteria.setSize(10000);
        criteria.setPage(0);
        
        List<Payment> payments = paymentAdminRepository.findPaymentsForExport(criteria, managerId);
        
        return generateExcelFile(payments);
    }

    /**
     * 결제 통계 정보 조회
     */
    public Map<String, Object> getPaymentStatistics(PaymentSearchCriteria criteria, CustomUserDetails userDetails) {
        validateUserPermission(userDetails);
        
        Long managerId = getManagerIdForFiltering(userDetails);
        
        Map<String, Object> statistics = new HashMap<>();
        
        // 결제 건수 통계
        Map<String, Long> countStats = paymentAdminRepository.getPaymentCountStatistics(criteria, managerId);
        statistics.put("paymentCounts", countStats);
        
        // 결제 금액 통계
        Map<String, BigDecimal> amountStats = paymentAdminRepository.getPaymentAmountStatistics(criteria, managerId);
        statistics.put("paymentAmounts", amountStats);
        
        // 결제 타입별 통계
        Map<String, Object> typeStats = paymentAdminRepository.getPaymentTypeStatistics(criteria, managerId);
        statistics.put("paymentTypes", typeStats);
        
        return statistics;
    }

    /**
     * 사용자 권한 검증
     */
    private void validateUserPermission(CustomUserDetails userDetails) {
        String roleCode = userDetails.getRoleCode();
        if (!"ADMIN".equals(roleCode) && !"EVENT_MANAGER".equals(roleCode)) {
            throw new AccessDeniedException("결제 관리 권한이 없습니다.");
        }
    }

    /**
     * 권한에 따른 managerId 필터링 값 결정
     */
    private Long getManagerIdForFiltering(CustomUserDetails userDetails) {
        String roleCode = userDetails.getRoleCode();
        
        if ("ADMIN".equals(roleCode)) {
            // 전체 관리자는 모든 데이터 조회 가능
            return null;
        } else if ("EVENT_MANAGER".equals(roleCode)) {
            // 행사 관리자는 본인이 관리하는 행사만 조회 가능
            return userDetails.getUserId();
        }
        
        throw new AccessDeniedException("잘못된 권한입니다.");
    }

    /**
     * 정렬 조건 생성
     */
    private Sort createSort(String sortField, String direction) {
        Sort.Direction sortDirection = "asc".equalsIgnoreCase(direction) 
            ? Sort.Direction.ASC 
            : Sort.Direction.DESC;
        
        // 정렬 필드 매핑
        String actualField;
        switch (sortField) {
            case "paidAt":
                actualField = "paidAt";
                break;
            case "amount":
                actualField = "amount";
                break;
            case "eventName":
                actualField = "event.titleKr";
                break;
            case "buyerName":
                actualField = "user.name";
                break;
            case "paymentStatus":
                actualField = "paymentStatusCode.name";
                break;
            default:
                actualField = "paidAt";
        }
        
        return Sort.by(sortDirection, actualField);
    }

    /**
     * 엑셀 파일 생성
     */
    private byte[] generateExcelFile(List<Payment> payments) {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("결제 내역");
            
            // 헤더 생성
            Row headerRow = sheet.createRow(0);
            String[] headers = {
                "결제ID", "주문번호", "행사명", "결제항목", "구매자명", 
                "결제금액", "결제상태", "결제일시", "환불금액", "환불일시"
            };
            
            for (int i = 0; i < headers.length; i++) {
                headerRow.createCell(i).setCellValue(headers[i]);
            }
            
            // 데이터 행 생성
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            
            for (int i = 0; i < payments.size(); i++) {
                Payment payment = payments.get(i);
                Row dataRow = sheet.createRow(i + 1);
                
                dataRow.createCell(0).setCellValue(payment.getPaymentId());
                dataRow.createCell(1).setCellValue(payment.getMerchantUid());
                dataRow.createCell(2).setCellValue(payment.getEvent() != null ? payment.getEvent().getTitleKr() : "");
                dataRow.createCell(3).setCellValue(payment.getPaymentTargetType().getPaymentTargetName());
                dataRow.createCell(4).setCellValue(payment.getUser().getName());
                dataRow.createCell(5).setCellValue(payment.getAmount().doubleValue());
                dataRow.createCell(6).setCellValue(payment.getPaymentStatusCode().getName());
                dataRow.createCell(7).setCellValue(payment.getPaidAt() != null ? payment.getPaidAt().format(formatter) : "");
                dataRow.createCell(8).setCellValue(payment.getRefundedAmount().doubleValue());
                dataRow.createCell(9).setCellValue(payment.getRefundedAt() != null ? payment.getRefundedAt().format(formatter) : "");
            }
            
            // 열 너비 자동 조정
            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }
            
            // ByteArray로 변환
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            workbook.write(outputStream);
            return outputStream.toByteArray();
            
        } catch (IOException e) {
            throw new RuntimeException("엑셀 파일 생성 중 오류가 발생했습니다.", e);
        }
    }
}