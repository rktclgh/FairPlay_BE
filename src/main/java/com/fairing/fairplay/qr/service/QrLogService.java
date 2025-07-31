package com.fairing.fairplay.qr.service;

import com.fairing.fairplay.qr.repository.QrActionCodeRepository;
import com.fairing.fairplay.qr.repository.QrCheckLogRepository;
import com.fairing.fairplay.qr.repository.QrCheckStatusCodeRepository;
import com.fairing.fairplay.qr.repository.QrLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class QrLogService {

  private final QrLogRepository qrLogRepository;
  private final QrCheckLogRepository qrCheckLogRepository;
  private final QrActionCodeRepository qrActionCodeRepository;
  private final QrCheckStatusCodeRepository qrCheckStatusCodeRepository;
}
