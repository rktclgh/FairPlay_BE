package com.fairing.fairplay.settlement.util;

import com.fairing.fairplay.settlement.dto.EventManagerSettlementListDto;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;

public class ExcelExporter {

    public static byte[] exportSettlementList(List<EventManagerSettlementListDto> settlements) throws IOException {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Settlements");
            int rowIdx = 0;

            // 헤더 스타일
            CellStyle headerStyle = workbook.createCellStyle();
            Font font = workbook.createFont();
            font.setBold(true);
            headerStyle.setFont(font);

            // 헤더 작성
            Row header = sheet.createRow(rowIdx++);
            String[] headers = {"정산 ID", "행사 ID", "행사명", "최종 금액", "승인 상태", "이의 상태", "송금 상태"};
            for (int i = 0; i < headers.length; i++) {
                Cell cell = header.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            // 데이터 작성
            for (EventManagerSettlementListDto dto : settlements) {
                Row row = sheet.createRow(rowIdx++);
                row.createCell(0).setCellValue(dto.getSettlementId());
                row.createCell(1).setCellValue(dto.getEventId());
                row.createCell(2).setCellValue(dto.getEventTitle());
                row.createCell(3).setCellValue(dto.getFinalAmount().doubleValue());
                row.createCell(4).setCellValue(dto.getAdminApprovalStatus().name());
                row.createCell(5).setCellValue(dto.getDisputeStatus().name());
                row.createCell(6).setCellValue(dto.getTransferStatus().name());
            }

            // AutoSize Columns
            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }

            // Byte 변환
            try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
                workbook.write(out);
                return out.toByteArray();
            }
        }
    }
}

