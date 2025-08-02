package com.fairing.fairplay.scheduler;

import com.fairing.fairplay.qr.service.QrTicketService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class QrTicketScheduler {

  private final QrTicketService qrTicketService;

//  // 매일 오전 9시 티켓 전송
//  @Scheduled(cron = "0 0 09 * * *")
//  public void runSendQrTicket(){
//    qrTicketService.generateQrLink();
//  }
}
