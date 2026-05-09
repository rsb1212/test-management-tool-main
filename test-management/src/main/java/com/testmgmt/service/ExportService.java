package com.testmgmt.service;

import com.itextpdf.text.*;
import com.itextpdf.text.pdf.*;
import com.testmgmt.entity.Defect;
import com.testmgmt.entity.Execution;
import com.testmgmt.exception.GlobalExceptionHandler.ResourceNotFoundException;
import com.testmgmt.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ExportService {

    private final ExecutionRepository executionRepo;
    private final DefectRepository    defectRepo;
    private final TestRunRepository   testRunRepo;

    private static final DateTimeFormatter FMT =
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm").withZone(ZoneId.systemDefault());

    // ── Export execution summary as PDF ───────────────────────
    @Transactional(readOnly = true)
    public byte[] exportExecutionPdf(UUID runId) {
        var run = testRunRepo.findById(runId)
            .orElseThrow(() -> new ResourceNotFoundException("TestRun not found: " + runId));
        List<Execution> executions = executionRepo.findByTestRun_Id(runId);

        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            Document doc = new Document(PageSize.A4.rotate());
            PdfWriter.getInstance(doc, baos);
            doc.open();

            // Title
            Font titleFont = (Font) FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16, BaseColor.DARK_GRAY);
            Font headerFont = (Font) FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, BaseColor.WHITE);
            Font cellFont  = (Font) FontFactory.getFont(FontFactory.HELVETICA, 9, BaseColor.BLACK);

            Paragraph title = new Paragraph("Test Execution Report — " + run.getCode(), (com.itextpdf.text.Font) titleFont);
            title.setAlignment(Element.ALIGN_CENTER);
            title.setSpacingAfter(10f);
            doc.add(title);

            Paragraph meta = new Paragraph(
                "Environment: " + (run.getEnvironment() != null ? run.getEnvironment() : "N/A")
                + "  |  Build: " + (run.getBuildVersion() != null ? run.getBuildVersion() : "N/A")
                + "  |  Generated: " + FMT.format(Instant.now()),
                FontFactory.getFont(FontFactory.HELVETICA, 9, BaseColor.GRAY));
            meta.setAlignment(Element.ALIGN_CENTER);
            meta.setSpacingAfter(16f);
            doc.add(meta);

            // Table
            PdfPTable table = new PdfPTable(6);
            table.setWidthPercentage(100);
            table.setWidths(new float[]{1.5f, 4f, 2f, 2f, 2f, 3f});

            // Headers
            String[] headers = {"Code", "Test Case", "Result", "Executed By", "Executed At", "Comment"};
            BaseColor headerBg = new BaseColor(52, 73, 94);
            for (String h : headers) {
                PdfPCell cell = new PdfPCell(new Phrase(h, (com.itextpdf.text.Font) headerFont));
                cell.setBackgroundColor(headerBg);
                cell.setPadding(6);
                cell.setHorizontalAlignment(Element.ALIGN_CENTER);
                table.addCell(cell);
            }

            // Rows
            for (Execution e : executions) {
                BaseColor rowBg = e.getResult() == Execution.ExecResult.PASSED
                    ? new BaseColor(212, 237, 218)
                    : e.getResult() == Execution.ExecResult.FAILED
                        ? new BaseColor(248, 215, 218)
                        : new BaseColor(255, 255, 255);

                String[] rowData = {
                    e.getCode(),
                    e.getTestCase().getTitle(),
                    e.getResult().name(),
                    e.getExecutedBy() != null ? e.getExecutedBy().getUsername() : "N/A",
                    e.getExecutedAt() != null ? FMT.format(e.getExecutedAt()) : "N/A",
                    e.getComment() != null ? e.getComment() : ""
                };
                for (String val : rowData) {
                    PdfPCell cell = new PdfPCell(new Phrase(val, (com.itextpdf.text.Font) cellFont));
                    cell.setBackgroundColor(rowBg);
                    cell.setPadding(5);
                    table.addCell(cell);
                }
            }

            doc.add(table);
            doc.close();
            return baos.toByteArray();

        } catch (Exception ex) {
            log.error("PDF generation failed", ex);
            throw new RuntimeException("Failed to generate PDF: " + ex.getMessage(), ex);
        }
    }

    // ── Export defects as Excel ───────────────────────────────
    @Transactional(readOnly = true)
    public byte[] exportDefectsExcel(UUID projectId) {
        List<Defect> defects = defectRepo.findAll(); // filtered in real use

        try (Workbook wb = new XSSFWorkbook();
             ByteArrayOutputStream baos = new ByteArrayOutputStream()) {

            Sheet sheet = wb.createSheet("Defects");

            // Header style
            CellStyle headerStyle = wb.createCellStyle();
            Font hFont = wb.createFont();
            hFont.setBold(true);
            hFont.setFontHeightInPoints((short) 11);
            headerStyle.setFont(hFont);
            headerStyle.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            Font whiteFont = wb.createFont();
            whiteFont.setBold(true);
            whiteFont.setColor(IndexedColors.WHITE.getIndex());
            headerStyle.setFont(whiteFont);

            // Create header row
            Row hRow = sheet.createRow(0);
            String[] cols = {"Code", "Title", "Severity", "Priority", "Status",
                             "Module", "Build", "Environment", "Jira Key", "Reported At"};
            for (int i = 0; i < cols.length; i++) {
                Cell cell = hRow.createCell(i);
                cell.setCellValue(cols[i]);
                cell.setCellStyle(headerStyle);
            }

            // Data rows
            int rowNum = 1;
            for (Defect d : defects) {
                Row row = sheet.createRow(rowNum++);
                row.createCell(0).setCellValue(d.getCode());
                row.createCell(1).setCellValue(d.getTitle());
                row.createCell(2).setCellValue(d.getSeverity().name());
                row.createCell(3).setCellValue(d.getPriority().name());
                row.createCell(4).setCellValue(d.getStatus().name());
                row.createCell(5).setCellValue(d.getModule() != null ? d.getModule() : "");
                row.createCell(6).setCellValue(d.getBuildVersion() != null ? d.getBuildVersion() : "");
                row.createCell(7).setCellValue(d.getEnvironment() != null ? d.getEnvironment() : "");
                row.createCell(8).setCellValue(d.getJiraIssueKey() != null ? d.getJiraIssueKey() : "");
                row.createCell(9).setCellValue(d.getCreatedAt() != null ? FMT.format(d.getCreatedAt()) : "");
            }

            // Auto-size columns
            for (int i = 0; i < cols.length; i++) sheet.autoSizeColumn(i);

            wb.write(baos);
            return baos.toByteArray();

        } catch (Exception ex) {
            log.error("Excel generation failed", ex);
            throw new RuntimeException("Failed to generate Excel: " + ex.getMessage(), ex);
        }
    }
}
