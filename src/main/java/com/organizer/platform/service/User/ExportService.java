package com.organizer.platform.service.User;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.organizer.platform.model.organizedDTO.MessageDTO;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;

@Service
public class ExportService {

    public byte[] generateExportFile(List<MessageDTO> messages) {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Messages");

            // Create header row with styles
            Row headerRow = sheet.createRow(0);
            CellStyle headerStyle = createHeaderStyle(workbook);

            // Define headers
            String[] headers = {
                    "Message Content", "Category",
                    "Sub Category", "Type", "Purpose", "Tags",
                    "Next Steps", "MIME Type"
            };

            // Create header cells
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
                sheet.autoSizeColumn(i);
            }

            // Create data rows
            int rowNum = 1;
            for (MessageDTO message : messages) {
                Row row = sheet.createRow(rowNum++);


                row.createCell(0).setCellValue(message.getMessageContent());
                row.createCell(1).setCellValue(message.getCategory());
                row.createCell(2).setCellValue(message.getSubCategory());
                row.createCell(3).setCellValue(message.getType());
                row.createCell(4).setCellValue(message.getPurpose());
                row.createCell(5).setCellValue(message.getTags() != null ?
                        String.join(", ", message.getTags()) : "");
                row.createCell(6).setCellValue(message.getNextSteps() != null ?
                        String.join(", ", message.getNextSteps()) : "");
                row.createCell(7).setCellValue(message.getMime());


            }

            // Auto-size columns for better readability
            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }

            // Convert workbook to byte array
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            workbook.write(outputStream);
            return outputStream.toByteArray();

        } catch (IOException e) {
            throw new RuntimeException("Failed to generate Excel file", e);
        }
    }

    private CellStyle createHeaderStyle(Workbook workbook) {
        CellStyle headerStyle = workbook.createCellStyle();

        // Set background color
        headerStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

        // Set border
        headerStyle.setBorderBottom(BorderStyle.THIN);
        headerStyle.setBorderTop(BorderStyle.THIN);
        headerStyle.setBorderRight(BorderStyle.THIN);
        headerStyle.setBorderLeft(BorderStyle.THIN);

        // Set font
        Font headerFont = workbook.createFont();
        headerFont.setBold(true);
        headerStyle.setFont(headerFont);

        return headerStyle;
    }
}
