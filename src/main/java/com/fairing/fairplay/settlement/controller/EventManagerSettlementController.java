package com.fairing.fairplay.settlement.controller;

import com.fairing.fairplay.core.etc.FunctionAuth;
import com.fairing.fairplay.core.security.CustomUserDetails;
import com.fairing.fairplay.settlement.dto.AccountRequestDto;
import com.fairing.fairplay.settlement.dto.ApproveEventManagerSettlementDto;
import com.fairing.fairplay.settlement.dto.EventManagerDetailSettlementDto;
import com.fairing.fairplay.settlement.dto.EventManagerSettlementListDto;
import com.fairing.fairplay.settlement.service.EventManagerSettlementService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/settlement")
@RequiredArgsConstructor
public class EventManagerSettlementController {

    private final EventManagerSettlementService eventManagerSettlementService;

    @GetMapping("/list")
    @FunctionAuth("getAllEventManagerSettlementList")
    public Page<EventManagerSettlementListDto> getAllEventManagerSettlementList(
            @PageableDefault(size = 10) Pageable pageable,
            @AuthenticationPrincipal CustomUserDetails userDetails)
    {
        return eventManagerSettlementService.getAllSettlement(userDetails.getUserId(),pageable);
    }

    @GetMapping("/{settlementId}")
    @FunctionAuth("getDetailEventManagerSettlementList")
    public EventManagerDetailSettlementDto getDetailEventManagerSettlementList(
            @PathVariable Long settlementId)
    {
        return eventManagerSettlementService.getDetailSettlement(settlementId);
    }

    @PostMapping("/{settlementId}/approve")
    @FunctionAuth("getApproveEventManagerSettlement")
    public ApproveEventManagerSettlementDto getApproveEventManagerSettlement(
            @PathVariable Long settlementId,
            @AuthenticationPrincipal(expression = "userId") Long userId)
    {
        return eventManagerSettlementService.approveEventManagerSettlement(settlementId,userId);
    }

    @PostMapping("/{settlementId}/registerAccount")
    @FunctionAuth("registerAccount")
    public ResponseEntity<Long> registerAccount(
            @Valid @RequestBody AccountRequestDto accountRequestDto,
            @PathVariable Long settlementId,
            @AuthenticationPrincipal(expression = "userId") Long userId)
    {
        Long id = eventManagerSettlementService.registerAccount(settlementId,accountRequestDto, userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(id);
    }
}
