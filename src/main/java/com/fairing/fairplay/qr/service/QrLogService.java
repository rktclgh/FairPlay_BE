package com.fairing.fairplay.qr.service;

import com.fairing.fairplay.common.exception.CustomException;
import com.fairing.fairplay.qr.entity.QrActionCode;
import com.fairing.fairplay.qr.entity.QrCheckLog;
import com.fairing.fairplay.qr.entity.QrCheckStatusCode;
import com.fairing.fairplay.qr.entity.QrLog;
import com.fairing.fairplay.qr.entity.QrTicket;
import com.fairing.fairplay.qr.repository.QrActionCodeRepository;
import com.fairing.fairplay.qr.repository.QrCheckLogRepository;
import com.fairing.fairplay.qr.repository.QrCheckStatusCodeRepository;
import com.fairing.fairplay.qr.repository.QrLogRepository;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/*
* 서비스	책임
QrTicketEntryService	입장/퇴장 요청 처리의 “흐름 제어”
- 유저/참석자/티켓 조회
- 권한 검증
- 중복 스캔·재입장 정책 적용
- 검증 통과 후 LogService 호출
QrLogService	로그 데이터 생성/저장/조회
- DB 접근 전 code 유효성 검증
- 상태 전이 가능 여부 확인 (마지막 로그 조회)
- 중복 기록 방지(시간 제한)
- 로그 생성 시 필요한 엔티티 생성·저장
* */


// QR과 관련된 로그 서비스
@Service
@RequiredArgsConstructor
public class QrLogService {

  private final QrLogRepository qrLogRepository;
  private final QrCheckLogRepository qrCheckLogRepository;
  private final QrActionCodeRepository qrActionCodeRepository;
  private final QrCheckStatusCodeRepository qrCheckStatusCodeRepository;
  private final QrEntryValidateService qrEntryValidateService;

  // QR 코드 발급
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void issuedQrLog(List<QrTicket> qrTickets) {
    QrActionCode qrActionCode = validateQrActionCode(QrActionCode.ISSUED);
    saveQrLog(qrTickets, qrActionCode);
  }

  // QR 코드 스캔
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void scannedQrLog(QrTicket qrTicket, String checkStatus) {
    QrActionCode qrActionCode = validateQrActionCode(checkStatus);
    // 중복 스캔 -> QrLog: invalid, QrChecktLog: duplicate 저장
    qrEntryValidateService.preventDuplicateScan(qrTicket, checkStatus);
    // 중복 스캔 아닐 경우 ENTRY 스캔을 위해 QrLog: scanned만 저장
    saveQrLog(qrTicket, qrActionCode);
  }

  // 입장 (QR 코드 스캔+수동 코드)
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public LocalDateTime entryQrLog(QrTicket qrTicket, String actionCodeType,
      String checkStatusCode) {
    // actionCodeType = CHECKED_IN, MANUAL_CHECKED_IN
    QrActionCode qrActionCode = validateQrActionCode(actionCodeType);
    // checkStatusCode = ENTRY, REENTRY
    QrCheckStatusCode qrCheckStatusCode = validateQrCheckStatusCode(
        checkStatusCode);
    // 잘못된 입퇴장 스캔 -> ENTRY, REENTRY
    qrEntryValidateService.preventInvalidScan(qrTicket, checkStatusCode);
    // qrLog: CHECKED_IN or MANUAL_CHECKED_IN 저장
    LocalDateTime entryTime = saveQrLog(qrTicket, qrActionCode);
    // qrCheckLog: ENTRY or REENTRY 저장
    saveQrCheckLog(qrTicket, qrCheckStatusCode);
    return entryTime;
  }

  // 퇴장 (QR 코드 스캔 or 수동 코드)
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void exitQrLog(QrTicket qrTicket) {
    // checkStatusCode = EXIT
    QrCheckStatusCode qrCheckStatusCode = validateQrCheckStatusCode(
        QrCheckStatusCode.EXIT);
    // 잘못된 입퇴장 스캔
    qrEntryValidateService.preventInvalidScan(qrTicket, QrCheckStatusCode.EXIT);
    // qrCheckLog: EXIT 저장
    saveQrCheckLog(qrTicket, qrCheckStatusCode);
  }

  // 비정상 중복 스캔 -> 스캔할 때 검토
  public void duplicateQrLog(QrTicket qrTicket) {
    QrActionCode invalidQrActionCode = validateQrActionCode(
        QrActionCode.INVALID);
    QrCheckStatusCode duplicateQrCheckStatusCode = validateQrCheckStatusCode(
        QrCheckStatusCode.DUPLICATE);

    saveQrLog(qrTicket, invalidQrActionCode);
    saveQrCheckLog(qrTicket, duplicateQrCheckStatusCode);
  }

  // 잘못된 스캔 -> 입장, 퇴장 시 검토
  public void invalidQrLog(QrTicket qrTicket) {
    QrActionCode qrActionCode = validateQrActionCode(QrActionCode.INVALID);
    QrCheckStatusCode qrCheckStatusCode = validateQrCheckStatusCode(
        QrCheckStatusCode.INVALID);

    saveQrLog(qrTicket, qrActionCode);
    saveQrCheckLog(qrTicket, qrCheckStatusCode);
  }

  // 특정 qr 티켓의 최근 checkstatus 로그 조회
  public QrCheckLog hasCheckRecord(QrTicket qrTicket, String qrCheckStatus) {
    return qrCheckLogRepository.findTop1ByQrTicketAndCheckStatusCode_CodeOrderByCreatedAtDesc(
        qrTicket, qrCheckStatus).orElse(null);
  }

  // 특정 qr 티켓의 최근 qrActionCode 로그 조회
  public boolean hasQrRecord(QrTicket qrTicket, String qrActionCode) {
    return qrLogRepository.findTop1ByQrTicketAndActionCode_CodeOrderByCreatedAtDesc(qrTicket,
        qrActionCode).isPresent();
  }

  // 과거 로그 조회해 현재 스캔이 ENTRY인지 REENTRY인지 판단
  public String determineEntryOrReEntry(QrTicket qrTicket) {
    QrCheckLog lastLogOpt = qrCheckLogRepository.findTop1ByQrTicketOrderByCreatedAtDesc(qrTicket)
        .orElse(null);
    if (lastLogOpt == null) {
      // 이전 로그 기록이 없을 때 최초 입장 판단
      return QrCheckStatusCode.ENTRY;
    } else {
      // 직전 로그가 하나라도 있으면 재입장 판단
      return QrCheckStatusCode.REENTRY;
    }
  }

  // QR 로그 코드 검증
  private QrActionCode validateQrActionCode(String qrActionCode) {
    return qrActionCodeRepository.findByCode(qrActionCode)
        .orElseThrow(() -> new CustomException(HttpStatus.BAD_REQUEST, "QR 상태 코드가 올바르지 않습니다."));
  }

  // QR 체크인 코드 검증
  private QrCheckStatusCode validateQrCheckStatusCode(String qrCheckStatusCode) {
    return qrCheckStatusCodeRepository.findByCode(qrCheckStatusCode)
        .orElseThrow(() -> new CustomException(
            HttpStatus.BAD_REQUEST, "QR 체크인 상태 코드가 올바르지 않습니다."));
  }

  // QrLog 저장 - ISSUED, SCAN, CHECKED_IN, MANUAL_CHECKED_IN, INVALID
  private LocalDateTime saveQrLog(QrTicket qrTicket, QrActionCode qrActionCode) {
    QrLog qrLog = QrLog.builder()
        .qrTicket(qrTicket)
        .actionCode(qrActionCode)
        .build();
    qrLogRepository.save(qrLog);
    qrLogRepository.flush();
    return qrLog.getCreatedAt();
  }

  // QrLog 다건 저장
  private void saveQrLog(List<QrTicket> qrTickets, QrActionCode qrActionCode) {
    final int BATCH_SIZE = 500; // 성능/메모리 상황에 맞춰 조절
    for (int i = 0; i < qrTickets.size(); i += BATCH_SIZE) {
      int end = Math.min(i + BATCH_SIZE, qrTickets.size());
      List<QrTicket> batch = qrTickets.subList(i, end);

      List<QrLog> logs = batch.stream()
          .map(ticket -> QrLog.builder()
              .qrTicket(ticket)
              .actionCode(qrActionCode)
              .createdAt(LocalDateTime.now())
              .build())
          .toList();

      qrLogRepository.saveAll(logs);
      qrLogRepository.flush(); // 중간 flush로 메모리 사용량 줄임
    }
  }

  // QrCheckLog 저장 - CHECKIN, CHECKOUT
  private void saveQrCheckLog(QrTicket qrTicket, QrCheckStatusCode qrCheckStatusCode) {
    QrCheckLog qrCheckLog = QrCheckLog.builder()
        .qrTicket(qrTicket)
        .checkStatusCode(qrCheckStatusCode)
        .build();
    qrCheckLogRepository.save(qrCheckLog);
    qrCheckLogRepository.flush();
  }
}
