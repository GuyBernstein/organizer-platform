package com.organizer.platform.service.User;

import com.organizer.platform.model.organizedDTO.MessageDTO;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;

/**
 * This service handles the export of messages to Excel format, making it easier for users
 * to analyze message data outside the platform and integrate with other tools.
 * Using Excel as the export format ensures broad compatibility and easy data manipulation.
 */
@Service
public class ExportService {

    /**
     * Converts a list of messages into an Excel file format.
     * Returns bytes instead of a File object to:
     * 1. Allow for flexible handling in different storage/transmission scenarios
     * 2. Avoid leaving temporary files on the server
     * 3. Make it easier to stream directly to HTTP responses
     */
    public byte[] generateExportFile(List<MessageDTO> messages) {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Messages");

            // Apply styling to headers to improve readability and provide visual separation
            // between headers and data
            Row headerRow = sheet.createRow(0);
            CellStyle headerStyle = createHeaderStyle(workbook);

            // Headers are defined as an array to:
            // 1. Make it easy to modify the export structure
            // 2. Ensure consistency between header creation and data population
            // 3. Centralize the export schema definition
            String[] headers = {
                    "Message Content", "Category",
                    "Sub Category", "Type", "Purpose", "Tags",
                    "Next Steps", "MIME Type"
            };

            // Create header cells with consistent styling
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
                sheet.autoSizeColumn(i);
            }

            // Populate data rows, starting from index 1 to leave room for headers
            int rowNum = 1;
            for (MessageDTO message : messages) {
                Row row = sheet.createRow(rowNum++);

                // Direct mapping of message properties to columns
                // Tags and NextSteps are joined with commas for better readability in Excel
                // Null checks prevent NullPointerException for optional fields
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

            // Auto-size columns after data population to ensure all content is visible
            // This improves user experience by eliminating the need for manual column resizing
            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }

            // Convert to byte array for flexible handling downstream
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            workbook.write(outputStream);
            return outputStream.toByteArray();

        } catch (IOException e) {
            throw new RuntimeException("Failed to generate Excel file", e);
        }
    }

    /**
     * Creates a consistent header style to improve spreadsheet readability.
     * The grey background, borders, and bold font help users distinguish
     * headers from data rows and understand the structure of the export.
     * This styling approach follows common spreadsheet design patterns
     * that users are familiar with.
     */
    private CellStyle createHeaderStyle(Workbook workbook) {
        CellStyle headerStyle = workbook.createCellStyle();

        // Grey background provides visual separation from data
        headerStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

        // Borders help define the header section and individual columns
        headerStyle.setBorderBottom(BorderStyle.THIN);
        headerStyle.setBorderTop(BorderStyle.THIN);
        headerStyle.setBorderRight(BorderStyle.THIN);
        headerStyle.setBorderLeft(BorderStyle.THIN);

        // Bold font emphasizes that these are headers
        Font headerFont = workbook.createFont();
        headerFont.setBold(true);
        headerStyle.setFont(headerFont);

        return headerStyle;
    }
}
