package com.fairing.fairplay.settlement.util;

import com.fairing.fairplay.settlement.dto.EventManagerSettlementListDto;
import com.fairing.fairplay.temp.dto.sales.AllSalesDto;
import com.fairing.fairplay.temp.dto.sales.DailySalesDto;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.format.DateTimeFormatter;
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

    /**
     * 이벤트별 매출 데이터를 Excel로 내보내기
     */
    public static byte[] exportAllSalesList(List<AllSalesDto> salesList) throws IOException {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Event Sales");
            int rowIdx = 0;

            // 헤더 스타일
            CellStyle headerStyle = workbook.createCellStyle();
            Font font = workbook.createFont();
            font.setBold(true);
            headerStyle.setFont(font);

            // 헤더 작성
            Row header = sheet.createRow(rowIdx++);
            String[] headers = {"이벤트명", "시작일", "종료일", "총 매출", "수수료 (8%)", "순수익 (92%)"};
            for (int i = 0; i < headers.length; i++) {
                Cell cell = header.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            // 데이터 작성
            DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
            for (AllSalesDto dto : salesList) {
                Row row = sheet.createRow(rowIdx++);
                row.createCell(0).setCellValue(dto.getEventName());
                row.createCell(1).setCellValue(dto.getStartDate() != null ? dto.getStartDate().format(dateFormatter) : "");
                row.createCell(2).setCellValue(dto.getEndDate() != null ? dto.getEndDate().format(dateFormatter) : "");
                row.createCell(3).setCellValue(dto.getTotalAmount() != null ? dto.getTotalAmount().doubleValue() : 0.0);
                row.createCell(4).setCellValue(dto.getTotalFee() != null ? dto.getTotalFee().doubleValue() : 0.0);
                row.createCell(5).setCellValue(dto.getTotalRevenue() != null ? dto.getTotalRevenue().doubleValue() : 0.0);
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

    /**
     * 일별 매출 데이터를 Excel로 내보내기
     */
    public static byte[] exportDailySalesList(List<DailySalesDto> salesList) throws IOException {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Daily Sales");
            int rowIdx = 0;

            // 헤더 스타일
            CellStyle headerStyle = workbook.createCellStyle();
            Font font = workbook.createFont();
            font.setBold(true);
            headerStyle.setFont(font);

            // 헤더 작성
            Row header = sheet.createRow(rowIdx++);
            String[] headers = {"날짜", "예약 매출", "부스 매출", "광고 매출", "기타 매출", "총 매출", "총 건수"};
            for (int i = 0; i < headers.length; i++) {
                Cell cell = header.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            // 데이터 작성
            DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
            for (DailySalesDto dto : salesList) {
                Row row = sheet.createRow(rowIdx++);
                row.createCell(0).setCellValue(dto.getDate() != null ? dto.getDate().format(dateFormatter) : "");
                row.createCell(1).setCellValue(dto.getReservationAmount() != null ? dto.getReservationAmount().doubleValue() : 0.0);
                row.createCell(2).setCellValue(dto.getBoothAmount() != null ? dto.getBoothAmount().doubleValue() : 0.0);
                row.createCell(3).setCellValue(dto.getAdAmount() != null ? dto.getAdAmount().doubleValue() : 0.0);
                row.createCell(4).setCellValue(dto.getEtcAmount() != null ? dto.getEtcAmount().doubleValue() : 0.0);
                row.createCell(5).setCellValue(dto.getTotalAmount() != null ? dto.getTotalAmount().doubleValue() : 0.0);
                row.createCell(6).setCellValue(dto.getTotalCount() != null ? dto.getTotalCount() : 0L);
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

