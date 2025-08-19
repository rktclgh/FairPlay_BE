package com.fairing.fairplay.settlement.controller;


import com.fairing.fairplay.core.etc.FunctionAuth;
import com.fairing.fairplay.settlement.dto.EventManagerSettlementListDto;
import com.fairing.fairplay.settlement.entity.Settlement;
import com.fairing.fairplay.settlement.service.AdminSettlementService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;

@RestController
@RequestMapping("/api/admin/settlement")
@RequiredArgsConstructor
public class AdminSettlementController {

    private final AdminSettlementService adminSettlementService;

    @GetMapping("/search")
    @FunctionAuth("getSearchSettlement")
    public Page<EventManagerSettlementListDto> getSearchSettlement(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) String settlementStatus,
            @RequestParam(required = false) String disputeStatus,
            @RequestParam(required = true) String keyword,
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable // 기본 페이징 조건
    ) {
        return adminSettlementService.searchSettlement(startDate, endDate, keyword, settlementStatus, disputeStatus, pageable);
    }


    @GetMapping("/list")
    @FunctionAuth("getAllSettlement")
    public Page<EventManagerSettlementListDto> getAllSettlement(
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return adminSettlementService.getSettlementList(pageable);
    }

    @PostMapping("/approve/{settlementId}")
    @FunctionAuth("approveSettlement")
    public Settlement approveSettlement(
            @PathVariable Long settlementId,
            @AuthenticationPrincipal(expression = "userId") Long userId) {
        return adminSettlementService.approveSettlement(settlementId,userId);
    }


    @PostMapping("/reject/{settlementId}")
    @FunctionAuth("rejectSettlement")
    public Settlement rejectSettlement(
            @PathVariable Long settlementId,
            @AuthenticationPrincipal(expression = "userId") Long userId) {
        return adminSettlementService.rejectSettlement(settlementId,userId);
    }


    @GetMapping("/export")
    @FunctionAuth("exportSettlements")
    public ResponseEntity<byte[]> exportSettlements(
            @RequestParam LocalDate startDate,
            @RequestParam LocalDate endDate,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String settlementStatus,
            @RequestParam(required = false) String disputeStatus
    ) throws IOException {
        byte[] excelFile = adminSettlementService.exportSettlements(startDate, endDate, keyword, settlementStatus, disputeStatus);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
        headers.setContentDisposition(ContentDisposition.attachment()
                .filename("settlements.xlsx", StandardCharsets.UTF_8)
                .build());

        return new ResponseEntity<>(excelFile, headers, HttpStatus.OK);
    }

}
