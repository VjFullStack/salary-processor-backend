package com.salaryprocessor.service;

import com.itextpdf.kernel.colors.DeviceRgb;
import com.itextpdf.kernel.geom.PageSize;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.borders.Border;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import com.salaryprocessor.model.SalaryResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.text.DecimalFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Service
public class PDFGenerationService {

    private static final Logger log = LoggerFactory.getLogger(PDFGenerationService.class);
    private static final DecimalFormat DECIMAL_FORMAT = new DecimalFormat("#,##0.00");
    private static final DecimalFormat PERCENTAGE_FORMAT = new DecimalFormat("0.00%");
    private static final DeviceRgb HEADER_BACKGROUND = new DeviceRgb(220, 220, 220);

    /**
     * Generate a salary slip PDF for an employee
     * @param salaryResult The salary computation result
     * @return PDF content as byte array
     */
    public byte[] generateSalarySlip(SalaryResult salaryResult) throws Exception {
        log.info("Starting PDF generation for employee: ID={}, Name={}", 
                salaryResult.getEmployeeId(), 
                salaryResult.getEmployeeName() != null ? salaryResult.getEmployeeName() : "Unknown");
        
        // Validate salary result has required fields
        if (salaryResult == null) {
            throw new IllegalArgumentException("Salary result cannot be null");
        }
        
        if (salaryResult.getEmployeeId() == null || salaryResult.getEmployeeId().isEmpty()) {
            throw new IllegalArgumentException("Employee ID cannot be null or empty");
        }
        
        // Use a default name if employee name is null
        if (salaryResult.getEmployeeName() == null || salaryResult.getEmployeeName().isEmpty()) {
            log.warn("Employee name is null or empty for ID: {}, using default name", salaryResult.getEmployeeId());
            salaryResult.setEmployeeName("Employee " + salaryResult.getEmployeeId());
        }
        
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        
        try {
            // Create a new PDF document
            PdfWriter writer = new PdfWriter(baos);
            PdfDocument pdf = new PdfDocument(writer);
            Document document = new Document(pdf, PageSize.A4);
            
            log.debug("Created PDF document structure for employee: {}", salaryResult.getEmployeeId());
            
            // Add title
            Paragraph title = new Paragraph("SALARY SLIP")
                    .setFontSize(20)
                    .setBold()
                    .setTextAlignment(TextAlignment.CENTER);
            document.add(title);
            
            // Add month and year
            LocalDate now = LocalDate.now();
            String monthYear = now.format(DateTimeFormatter.ofPattern("MMMM yyyy"));
            Paragraph period = new Paragraph("For the month of " + monthYear)
                    .setFontSize(12)
                    .setTextAlignment(TextAlignment.CENTER);
            document.add(period);
            
            document.add(new Paragraph("\n"));
            
            // Create employee info table
            Table employeeTable = new Table(UnitValue.createPercentArray(new float[]{30, 70}));
            employeeTable.setWidth(UnitValue.createPercentValue(100));
            
            employeeTable.addCell(createHeaderCell("Employee ID:"));
            employeeTable.addCell(createValueCell(salaryResult.getEmployeeId()));
            
            employeeTable.addCell(createHeaderCell("Employee Name:"));
            employeeTable.addCell(createValueCell(salaryResult.getEmployeeName()));
            
            document.add(employeeTable);
            document.add(new Paragraph("\n"));
            
            // Create salary details table
            Table salaryTable = new Table(UnitValue.createPercentArray(new float[]{50, 50}));
            salaryTable.setWidth(UnitValue.createPercentValue(100));
            
            salaryTable.addCell(createHeaderCell("Monthly Salary:"));
            salaryTable.addCell(createValueCell("₹ " + DECIMAL_FORMAT.format(salaryResult.getMonthlySalary())));
            
            salaryTable.addCell(createHeaderCell("Expected Hours:"));
            salaryTable.addCell(createValueCell(DECIMAL_FORMAT.format(salaryResult.getExpectedHours()) + " hours"));
            
            salaryTable.addCell(createHeaderCell("Actual Worked Hours:"));
            salaryTable.addCell(createValueCell(DECIMAL_FORMAT.format(salaryResult.getActualWorkedHours()) + " hours"));
            
            salaryTable.addCell(createHeaderCell("Late Marks:"));
            salaryTable.addCell(createValueCell(String.valueOf(salaryResult.getLateMarks())));
            
            salaryTable.addCell(createHeaderCell("Coefficient:"));
            salaryTable.addCell(createValueCell(PERCENTAGE_FORMAT.format(salaryResult.getCoefficient())));
            
            // Add late mark penalty if there are late marks
            if (salaryResult.getLateMarks() > 0) {
                salaryTable.addCell(createHeaderCell("Late Mark Penalty:"));
                salaryTable.addCell(createValueCell(PERCENTAGE_FORMAT.format(salaryResult.getLateMarkPenalty())));
            }
            
            salaryTable.addCell(createHeaderCell("Final Payable Salary:").setBold());
            salaryTable.addCell(createValueCell("₹ " + DECIMAL_FORMAT.format(salaryResult.getFinalPayableSalary())).setBold());
            
            document.add(salaryTable);
            
            // Add late marks explanation if applicable
            if (salaryResult.getLateMarks() > 0) {
                document.add(new Paragraph("\n"));
                Paragraph lateMarksInfo = new Paragraph("Late Marks Information:")
                        .setBold()
                        .setFontSize(12);
                document.add(lateMarksInfo);
                
                String lateMarkText = "You have " + salaryResult.getLateMarks() + " late mark(s). ";
                if (salaryResult.getLateMarks() <= 3) {
                    lateMarkText += "First 3 late marks result in a 0.5 day salary deduction.";
                } else {
                    lateMarkText += "First 3 late marks result in a 0.5 day salary deduction. " +
                            "Each additional late mark results in a 0.5/3 day salary deduction.";
                }
                
                document.add(new Paragraph(lateMarkText));
            }
            
            // Add signature section
            document.add(new Paragraph("\n\n"));
            Table signatureTable = new Table(2).useAllAvailableWidth();
            signatureTable.addCell(new Cell().add(new Paragraph("Employee Signature")).setBorder(Border.NO_BORDER));
            signatureTable.addCell(new Cell().add(new Paragraph("Employer Signature")).setBorder(Border.NO_BORDER));
            document.add(signatureTable);
            
            // Close document
            document.close();
            log.info("PDF generation completed successfully for employee: {}", salaryResult.getEmployeeId());
            
        } catch (Exception e) {
            log.error("Error generating PDF for employee {}: {}", 
                    salaryResult.getEmployeeId(), e.getMessage(), e);
            throw new Exception("Failed to generate PDF for employee " + 
                    salaryResult.getEmployeeId() + ": " + e.getMessage(), e);
        }
        
        return baos.toByteArray();
    }
    
    /**
     * Generate a ZIP file containing PDF salary slips for all employees
     * @param salaryResults List of salary computation results
     * @return ZIP file content as byte array
     */
    public byte[] generateSalarySlipsZip(List<SalaryResult> salaryResults) throws IOException {
        log.info("Starting ZIP generation for {} salary results", salaryResults.size());
        
        // Validate salary results
        if (salaryResults == null || salaryResults.isEmpty()) {
            log.error("No salary results provided for ZIP generation");
            throw new IllegalArgumentException("Salary results cannot be null or empty");
        }
        
        // Log details of each salary result for debugging
        for (int i = 0; i < salaryResults.size(); i++) {
            SalaryResult result = salaryResults.get(i);
            log.info("Salary result #{}: employeeId={}, name={}, monthlySalary={}, finalSalary={}",
                    i+1, result.getEmployeeId(), 
                    result.getEmployeeName() != null ? result.getEmployeeName() : "Unknown",
                    result.getMonthlySalary(), result.getFinalPayableSalary());
        }
        
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ZipOutputStream zipOut = new ZipOutputStream(baos);
        
        int successCount = 0;
        int failureCount = 0;
        
        for (SalaryResult result : salaryResults) {
            try {
                // Skip null results
                if (result == null) {
                    log.warn("Skipping null salary result");
                    failureCount++;
                    continue;
                }
                
                // Ensure employeeId is not null
                if (result.getEmployeeId() == null || result.getEmployeeId().isEmpty()) {
                    log.warn("Skipping salary result with null or empty employee ID");
                    failureCount++;
                    continue;
                }
                
                // Create PDF file name
                String fileName = "Salary_Slip_" + result.getEmployeeId() + ".pdf";
                log.info("Generating PDF for employee: {}, name: {}", result.getEmployeeId(), 
                        result.getEmployeeName() != null ? result.getEmployeeName() : "Unknown");
                
                // Generate PDF
                byte[] pdfData = generateSalarySlip(result);
                
                // Verify PDF data is not null or empty
                if (pdfData == null || pdfData.length == 0) {
                    log.error("Failed to generate PDF for employee {}: Empty PDF data", result.getEmployeeId());
                    failureCount++;
                    continue;
                }
                
                // Add PDF to ZIP
                ZipEntry zipEntry = new ZipEntry(fileName);
                zipOut.putNextEntry(zipEntry);
                zipOut.write(pdfData);
                zipOut.closeEntry();
                log.info("Successfully added {} to ZIP file ({} bytes)", fileName, pdfData.length);
                successCount++;
            } catch (Exception e) {
                failureCount++;
                log.error("Error generating PDF for employee {}: {}", 
                        result != null ? result.getEmployeeId() : "unknown", e.getMessage(), e);
            }
        }
        
        log.info("ZIP file generation completed. Success: {}, Failures: {}", successCount, failureCount);
        
        zipOut.close();
        return baos.toByteArray();
    }

    /**
     * Create a header cell for the salary slip tables
     */
    private Cell createHeaderCell(String text) {
        Cell cell = new Cell();
        cell.setBackgroundColor(HEADER_BACKGROUND);
        cell.add(new Paragraph(text).setBold());
        return cell;
    }

    /**
     * Create a value cell for the salary slip tables
     */
    private Cell createValueCell(String text) {
        return new Cell().add(new Paragraph(text));
    }
}
