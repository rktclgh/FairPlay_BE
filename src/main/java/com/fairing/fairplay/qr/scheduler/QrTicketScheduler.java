package com.fairing.fairplay.qr.scheduler;

import com.fairing.fairplay.qr.service.QrTicketService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class QrTicketScheduler {

  private final QrTicketService qrTicketService;

//  // 매일 자정 QR 티켓 세팅
//  @Scheduled(cron = "0 0 0 * * *")
//  public void runCreateQrTicket() {
//    qrTicketService.createQrTicket();
//  }
//
//  // 매일 오전 9시 티켓 전송
//  @Scheduled(cron = "0 0 09 * * *")
//  public void runSendQrTicket(){
//    qrTicketService.generateQrLink();
//  }
}
