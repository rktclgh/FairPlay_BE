package com.fairing.fairplay.scheduler;

import com.fairing.fairplay.qr.service.QrTicketBatchService;
import com.fairing.fairplay.qr.service.QrTicketService;
import com.fairing.fairplay.shareticket.entity.ShareTicket;
import com.fairing.fairplay.shareticket.service.ShareTicketService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@EnableScheduling
@Slf4j
public class ShareTicketQrScheduler {

  private final ShareTicketService shareTicketService;
  private final QrTicketBatchService qrTicketBatchService;

  // 공유 폼 링크 만료
  /*
  @Scheduled(cron = "0 0 0 * * *")  //매일 자정 실행
  public void runDailyTasks() {
    int batchSize = 100; //
    int page = 0;
    int maxIterations = 1000; // 무한루프 방지
    int currentIteration = 0;

    // 1. 만료 처리
    while (true) {
      if (currentIteration >= maxIterations) {
        // 로깅 및 알림 로직 추가
        break;
      }

      try {
        List<ShareTicket> batch = shareTicketService.fetchExpiredBatch(page, batchSize);
        if (batch.isEmpty()) {
          break;
        }

        shareTicketService.expiredToken(batch);
        page++;

      } catch (Exception e) {
        log.info("expiredToken 오류 발생:{}",e.getMessage());
        page++;
        currentIteration++;
      }
    }
    // 2. 만료 완료 후 QR 티켓 세팅
    qrTicketBatchService.createQrTicket();
  }
*/
}
