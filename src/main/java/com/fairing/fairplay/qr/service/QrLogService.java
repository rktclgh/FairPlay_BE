package com.fairing.fairplay.qr.service;

import com.fairing.fairplay.common.exception.CustomException;
import com.fairing.fairplay.qr.entity.QrActionCode;
import com.fairing.fairplay.qr.entity.QrCheckLog;
import com.fairing.fairplay.qr.entity.QrCheckStatusCode;
import com.fairing.fairplay.qr.entity.QrLog;
import com.fairing.fairplay.qr.entity.QrTicket;
import com.fairing.fairplay.qr.repository.QrCheckLogRepository;

import com.fairing.fairplay.qr.repository.QrLogRepository;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

// QR과 관련된 로그 서비스
@Service
@RequiredArgsConstructor
@Slf4j
public class QrLogService {

  private final QrLogRepository qrLogRepository;
  private final QrCheckLogRepository qrCheckLogRepository;

  // QR 코드 다건 발급
  @Transactional
  public void issuedQrLogs(List<QrTicket> qrTickets, QrActionCode qrActionCode) {
    saveQrLog(qrTickets, qrActionCode);
  }

  @Transactional
  public void issuedQrLog(QrTicket qrTicket, QrActionCode qrActionCode) {
    saveQrLog(qrTicket, qrActionCode);
  }

  // QR 코드 스캔
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void scannedQrLog(QrTicket qrTicket, QrActionCode qrActionCode) {
    // ENTRY 스캔을 위해 QrLog: scanned만 저장
    saveQrLog(qrTicket, qrActionCode);
  }

  // 입장 (QR 코드 스캔+수동 코드)
  @Transactional
  public LocalDateTime entryQrLog(QrTicket qrTicket, QrActionCode qrActionCode,
      QrCheckStatusCode qrCheckStatusCode) {
    // qrLog: CHECKED_IN or MANUAL_CHECKED_IN 저장
    LocalDateTime entryTime = saveQrLog(qrTicket, qrActionCode);
    // qrCheckLog: ENTRY or REENTRY 저장
    saveQrCheckLog(qrTicket, qrCheckStatusCode);
    log.info("입장 저장 완료");
    log.info("entryTime: {}", entryTime);
    log.info("qrCheckStatusCode: {}", qrCheckStatusCode);
    return entryTime;
  }

  // 퇴장 (QR 코드 스캔 or 수동 코드)
  @Transactional
  public LocalDateTime exitQrLog(QrTicket qrTicket, QrCheckStatusCode qrCheckStatusCode) {
    // qrCheckLog: EXIT 저장
    saveQrCheckLog(qrTicket, qrCheckStatusCode);
    log.info("부스 입장 완료");
    log.info("qrCheckStatusCode: {}", qrCheckStatusCode);
    return null;
  }

  // 부스 입장 시 이전 상태가 저장되지 않았을 경우 로그 저장
  @Transactional
  public LocalDateTime boothQrLog(QrTicket qrTicket, QrCheckStatusCode prevCheckStatusCode) {
    saveQrCheckLog(qrTicket, prevCheckStatusCode);
    return null;
  }

  @Transactional
  public LocalDateTime forceCheckQrLog(QrTicket qrTicket, QrActionCode qrActionCode,
      QrCheckStatusCode qrCheckStatusCode) {
    saveQrLog(qrTicket, qrActionCode);
    saveQrCheckLog(qrTicket, qrCheckStatusCode);
    return null;
  }

  // 비정상 중복 스캔 -> 스캔할 때 검토
  public void duplicateQrLog(QrTicket qrTicket, QrActionCode qrActionCode,
      QrCheckStatusCode qrCheckStatusCode) {
    saveQrLog(qrTicket, qrActionCode);
    saveQrCheckLog(qrTicket, qrCheckStatusCode);
  }

  // 잘못된 스캔 -> 입장, 퇴장 시 검토
  public void invalidQrLog(QrTicket qrTicket, QrActionCode qrActionCode,
      QrCheckStatusCode qrCheckStatusCode) {
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
    int successCount = 0;
    int failCount = 0;
    for (int i = 0; i < qrTickets.size(); i += BATCH_SIZE) {
      int end = Math.min(i + BATCH_SIZE, qrTickets.size());
      List<QrTicket> batch = qrTickets.subList(i, end);
      log.info("Processing batch {}/{}: size={}",
          (i / BATCH_SIZE) + 1,
          (qrTickets.size() + BATCH_SIZE - 1) / BATCH_SIZE,
          batch.size());

      try {
        List<QrLog> logs = batch.stream()
            .map(ticket -> QrLog.builder()
                .qrTicket(ticket)
                .actionCode(qrActionCode)
                .createdAt(LocalDateTime.now())
                .build())
            .toList();
        qrLogRepository.saveAll(logs);
        qrLogRepository.flush();
        successCount += batch.size();
      } catch (Exception e) {
        log.error("Failed to save batch starting at index {}: {}", i, e.getMessage());
        failCount += batch.size();
      }
    }

    log.info("Batch processing completed: success={}, fail={}", successCount, failCount);
    if (failCount > 0) {
      throw new CustomException(HttpStatus.INTERNAL_SERVER_ERROR,
          String.format("일부 로그 저장 실패: %d건", failCount));
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
