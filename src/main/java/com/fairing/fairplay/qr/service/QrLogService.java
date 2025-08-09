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
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

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

  // QR 코드 발급
  public void issuedQrLog(QrTicket qrTicket) {
    QrActionCode qrActionCode = validateQrActionCode(QrActionCode.ISSUED);
    saveQrLog(qrTicket, qrActionCode);
  }

  // QR 코드 스캔
  public void scannedQrLog(QrTicket qrTicket, String checkStatus) {
    QrActionCode qrActionCode = validateQrActionCode(QrActionCode.SCANNED);
    saveQrLog(qrTicket, qrActionCode);

    // 중복 스캔
    preventDuplicateScan(qrTicket, checkStatus);
  }

  // 입장 (QR 코드 스캔+수동 코드)
  public void entryQrLog(QrTicket qrTicket, String checkInType, String entryType) {
    // checkInType = CHECKED_IN, MANUAL_CHECKED_IN
    QrActionCode qrActionCode = validateQrActionCode(checkInType);
    // entryType = ENTRY, REENTRY
    QrCheckStatusCode qrCheckStatusCode = validateQrCheckStatusCode(entryType);

    // 잘못된 입퇴장 스캔 -> ENTRY, REENTRY, EXIT
    preventInvalidScan(qrTicket, entryType);

    saveQrLog(qrTicket, qrActionCode);
    saveQrCheckLog(qrTicket, qrCheckStatusCode);
  }

  public void exitQrLog(QrTicket qrTicket) {
    QrActionCode qrActionCode = validateQrActionCode(QrActionCode.SCANNED);
    QrCheckStatusCode qrCheckStatusCode = validateQrCheckStatusCode(QrCheckStatusCode.EXIT);

    // 중복 스캔
    preventDuplicateScan(qrTicket, QrCheckStatusCode.EXIT);
    // 잘못된 입퇴장 스캔
    preventInvalidScan(qrTicket, QrCheckStatusCode.EXIT);

    saveQrLog(qrTicket, qrActionCode);
    saveQrCheckLog(qrTicket, qrCheckStatusCode);
  }

  // 비정상 중복 스캔
  public void duplicateQrLog(QrTicket qrTicket) {
    QrActionCode invalidQrActionCode = validateQrActionCode(QrActionCode.INVALID);
    QrCheckStatusCode duplicateQrCheckStatusCode = validateQrCheckStatusCode(
        QrCheckStatusCode.DUPLICATE);

    saveQrLog(qrTicket, invalidQrActionCode);
    saveQrCheckLog(qrTicket, duplicateQrCheckStatusCode);
  }

  // 잘못된 스캔
  public void invalidQrLog(QrTicket qrTicket) {
    QrActionCode qrActionCode = validateQrActionCode(QrActionCode.INVALID);
    QrCheckStatusCode qrCheckStatusCode = validateQrCheckStatusCode(QrCheckStatusCode.INVALID);

    saveQrLog(qrTicket, qrActionCode);
    saveQrCheckLog(qrTicket, qrCheckStatusCode);
  }

  // QrLog 저장 - ISSUED, SCAN, CHECKED_IN, MANUAL_CHECKED_IN, INVALID
  private void saveQrLog(QrTicket qrTicket, QrActionCode qrActionCode) {
    QrLog qrLog = QrLog.builder()
        .qrTicket(qrTicket)
        .actionCode(qrActionCode)
        .build();
    qrLogRepository.save(qrLog);
  }

  // QrCheckLog 저장 - CHECKIN, CHECKOUT
  private void saveQrCheckLog(QrTicket qrTicket, QrCheckStatusCode qrCheckStatusCode) {
    QrCheckLog qrCheckLog = QrCheckLog.builder()
        .qrTicket(qrTicket)
        .checkStatusCode(qrCheckStatusCode)
        .build();
    qrCheckLogRepository.save(qrCheckLog);
  }

  // 잘못된 순서로 스캔 ( checkStatus - 시도하려는 동작 )
  public void preventInvalidScan(QrTicket qrTicket, String checkStatus) {
    Optional<QrCheckLog> lastLogOpt = qrCheckLogRepository.findTop1ByQrTicketOrderByCreatedAtDesc(
        qrTicket);

    // 처음 입장하여 직전 로그가 없는데 EXIT 스캔하는 경우
    if (lastLogOpt.isEmpty()) {
      if (QrCheckStatusCode.EXIT.equals(checkStatus) || QrCheckStatusCode.REENTRY.equals(
          checkStatus)) {
        invalidQrLog(qrTicket);
        throw new CustomException(HttpStatus.BAD_REQUEST, "첫 스캔은 ENTRY만 허용됩니다.");
      }
      return;
    }

    boolean isValid = isQrCheckStatusCodeValid(checkStatus, lastLogOpt.get());

    // 만약 isValid가 false로 변경 -> 직전 statuscode가 올바르지 않음
    if (!isValid) {
      invalidQrLog(qrTicket);
      throw new CustomException(HttpStatus.BAD_REQUEST,
          "잘못된 입퇴장 동작입니다. 마지막 상태: "
              + lastLogOpt.get().getCheckStatusCode().getName());
    }
  }

  private boolean isQrCheckStatusCodeValid(String checkStatus, QrCheckLog lastLogOpt) {
    String lastStatus = lastLogOpt.getCheckStatusCode().getCode();

    boolean isValid = true;
    if (QrCheckStatusCode.ENTRY.equals(checkStatus)) {
      // ENTRY는 직전이 EXIT 여야 입장 가능
      isValid = QrCheckStatusCode.EXIT.equals(lastStatus);
    } else if (QrCheckStatusCode.REENTRY.equals(checkStatus)) {
      // REENTRY는 직전이 EXIT 여야 입장 가능
      isValid = QrCheckStatusCode.EXIT.equals(lastStatus);
    } else if (QrCheckStatusCode.EXIT.equals(checkStatus)) {
      // EXIT는 직전이 ENTRY, REENTRY 여야 입장 가능
      isValid = QrCheckStatusCode.ENTRY.equals(lastStatus) || QrCheckStatusCode.REENTRY.equals(
          lastStatus);
    }
    return isValid;
  }

  // 중복 스캔 방지 - 최근 기록을 보고 너무 짧은 시간 내의 동일 상태 코드를 막음
  private void preventDuplicateScan(QrTicket qrTicket, String statusCode) {
    Optional<QrCheckLog> lastLogOpt = qrCheckLogRepository.findTop1ByQrTicketAndCheckStatusCode_CodeOrderByCreatedAtDesc(
        qrTicket, statusCode);

    // 5초 간격으로 빠르게 스캔했을 경우
    if (lastLogOpt.isPresent()) {
      QrCheckLog lastLog = lastLogOpt.get();
      if (lastLog.getCreatedAt().isAfter(LocalDateTime.now().minusSeconds(5))) {
        duplicateQrLog(qrTicket);
        throw new CustomException(HttpStatus.CONFLICT, "QR 코드를 너무 빠르게 스캔하였습니다.");
      }
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
}
