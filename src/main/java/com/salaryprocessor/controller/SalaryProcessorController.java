package com.salaryprocessor.controller;

import com.salaryprocessor.model.AttendanceRecord;
import com.salaryprocessor.model.Employee;
import com.salaryprocessor.model.SalaryResult;
import com.salaryprocessor.service.ContentfulService;
import com.salaryprocessor.service.ExcelParserService;
import com.salaryprocessor.service.PDFGenerationService;
import com.salaryprocessor.service.SalaryComputationService;
// import lombok.RequiredArgsConstructor; // Removed to use explicit constructor
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DateUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/salary")
public class SalaryProcessorController {
    
    private static final Logger log = LoggerFactory.getLogger(SalaryProcessorController.class);

    @Value("${file.upload.dir:uploads}")
    private String fileUploadDir;
    
    @Autowired
    private ExcelParserService excelParserService;
    
    @Autowired
    private SalaryComputationService salaryComputationService;
    
    @Autowired
    private ContentfulService contentfulService;
    
    /**
     * Set the total working days for salary calculation
     * @param days The total working days to set
     * @return Current setting after update
     */
    @PostMapping("/set-total-days")
    public ResponseEntity<Map<String, Object>> setTotalWorkingDays(@RequestParam("days") int days) {
        log.info("Request to set total working days to: {}", days);
        salaryComputationService.setTotalWorkingDays(days);
        
        Map<String, Object> response = new HashMap<>();
        response.put("totalWorkingDays", salaryComputationService.getTotalWorkingDays());
        response.put("status", "success");
        response.put("message", "Total working days updated successfully");
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * Get the current total working days setting
     * @return Current total working days value
     */
    @GetMapping("/total-days")
    public ResponseEntity<Map<String, Object>> getTotalWorkingDays() {
        int days = salaryComputationService.getTotalWorkingDays();
        log.info("Current total working days setting: {}", days);
        
        Map<String, Object> response = new HashMap<>();
        response.put("totalWorkingDays", days);
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * Get all employees from Contentful
     * @return List of all employees
     */
    @GetMapping("/employees")
    public ResponseEntity<List<Employee>> getAllEmployees() {
        log.info("Fetching all employees from Contentful");
        List<Employee> employees = contentfulService.getAllEmployees();
        return ResponseEntity.ok(employees);
    }
    
    /**
     * Get an employee by ID from Contentful
     * @param employeeId The ID of the employee to fetch
     * @return The employee with the given ID
     */
    @GetMapping("/employees/{employeeId}")
    public ResponseEntity<Employee> getEmployeeById(@PathVariable String employeeId) {
        log.info("Fetching employee with ID: {} from Contentful", employeeId);
        Employee employee = contentfulService.getEmployeeById(employeeId);
        return ResponseEntity.ok(employee);
    }
    
    /**
     * Refresh employee data from Contentful
     * @return Success message
     */
    @PostMapping("/employees/refresh")
    public ResponseEntity<Map<String, Object>> refreshEmployeeData() {
        log.info("Refreshing employee data from Contentful");
        contentfulService.refreshEmployeeData();
        
        Map<String, Object> response = new HashMap<>();
        response.put("status", "success");
        response.put("message", "Employee data refreshed from Contentful");
        response.put("employeeCount", contentfulService.getAllEmployees().size());
        
        return ResponseEntity.ok(response);
    }
    
    @Autowired
    private PDFGenerationService pdfGenerationService;
    
    // Cache to store the last processed salary results for PDF generation
    private Map<String, SalaryResult> lastProcessedResults = new HashMap<>();
    
    /**
     * Special method to detect and extract employee data in the format "2 : Manjiri Desai"
     * @param file The Excel file to analyze
     * @return Map of employee IDs to their attendance records
     */
    private Map<String, List<AttendanceRecord>> lookForColonPatternInExcel(MultipartFile file) throws Exception {
        log.info("Looking for colon pattern in Excel file: {}", file.getOriginalFilename());
        Map<String, List<AttendanceRecord>> result = new HashMap<>();
        
        try (Workbook workbook = WorkbookFactory.create(file.getInputStream())) {
            // Use first sheet by default
            Sheet sheet = workbook.getSheetAt(0);
            log.info("Analyzing sheet {} with {} rows", sheet.getSheetName(), sheet.getPhysicalNumberOfRows());
            
            // Look through all rows for the pattern
            for (int i = 0; i < sheet.getPhysicalNumberOfRows(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) continue;
                
                // Try to find cells with the pattern "ID : Name"
                for (int j = 0; j < row.getLastCellNum(); j++) {
                    Cell cell = row.getCell(j);
                    if (cell == null) continue;
                    
                    String cellValue = getCellValueAsString(cell);
                    if (cellValue == null || cellValue.isEmpty()) continue;
                    
                    log.info("Row {}, Col {}: '{}'", i, j, cellValue);
                    
                    // Check for the pattern "ID : Name"
                    if (cellValue.contains(":")) {
                        log.info("Found colon pattern in cell: {}", cellValue);
                        String[] parts = cellValue.split(":", 2);
                        if (parts.length == 2) {
                            String employeeId = parts[0].trim();
                            String employeeName = parts[1].trim();
                            log.info("Extracted Employee ID: '{}', Name: '{}'", employeeId, employeeName);
                            
                            // Create a default attendance record for this employee
                            AttendanceRecord record = new AttendanceRecord();
                            record.setEmployeeId(employeeId);
                            record.setEmployeeName(employeeName);
                            record.setDate(LocalDate.now()); // Default to today
                            record.setStatus("P"); // Default to Present
                            record.setHoursWorked(8.0); // Default to 8 hours
                            
                            // Try to find date in the same row
                            for (int k = 0; k < row.getLastCellNum(); k++) {
                                Cell dateCell = row.getCell(k);
                                if (dateCell != null && dateCell.getCellType() == CellType.NUMERIC && 
                                    DateUtil.isCellDateFormatted(dateCell)) {
                                    Date date = dateCell.getDateCellValue();
                                    record.setDate(date.toInstant().atZone(ZoneId.systemDefault()).toLocalDate());
                                    log.info("Found date: {}", record.getDate());
                                }
                            }
                            
                            // Add the record to the result map
                            if (!result.containsKey(employeeId)) {
                                result.put(employeeId, new ArrayList<>());
                            }
                            result.get(employeeId).add(record);
                            log.info("Added record for employee ID: {}", employeeId);
                        }
                    }
                }
            }
        }
        
        log.info("Found {} employees with the colon pattern", result.size());
        return result;
    }
    
    /**
     * Helper method to get cell value as string regardless of type
     */
    private String getCellValueAsString(Cell cell) {
        if (cell == null) {
            return "";
        }
        
        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue();
            case NUMERIC:
                if (DateUtil.isCellDateFormatted(cell)) {
                    return cell.getDateCellValue().toString();
                }
                return String.valueOf(cell.getNumericCellValue());
            case BOOLEAN:
                return String.valueOf(cell.getBooleanCellValue());
            case FORMULA:
                try {
                    return cell.getStringCellValue();
                } catch (Exception e) {
                    try {
                        return String.valueOf(cell.getNumericCellValue());
                    } catch (Exception ex) {
                        return "#FORMULA_ERROR#";
                    }
                }
            default:
                return "";
        }
    }

    /**
     * Process an Excel file and return salary computation results
     * @param file The Excel file with attendance data
     * @return JSON with salary computation results
     */
    @PostMapping("/process")
    public ResponseEntity<List<SalaryResult>> processSalary(
        @RequestParam("file") MultipartFile file,
        @RequestParam(value = "totalDays", required = false, defaultValue = "30") int totalDays) {
        try {
            log.info("Processing salary data from file: {}, size: {} bytes, content type: {}", 
                file.getOriginalFilename(), file.getSize(), file.getContentType());
                
            // Add extreme debugging to see what's happening with the file
            if (file == null) {
                log.error("File is null!");
                return ResponseEntity.badRequest().build();
            }
            
            if (file.isEmpty()) {
                log.error("File is empty (size: {})", file.getSize());
                return ResponseEntity.badRequest().build();
            }
            
            log.info("File details - name: {}, originalFilename: {}, contentType: {}, size: {}", 
                file.getName(), file.getOriginalFilename(), file.getContentType(), file.getSize());
            Map<String, List<AttendanceRecord>> attendanceRecords = excelParserService.parseExcel(file);
            
            log.info("Parsed attendance records: {} employees, {} total records", 
                attendanceRecords.size(), 
                attendanceRecords.values().stream().mapToInt(List::size).sum());
            
            log.info("Employee IDs found in Excel: {}", attendanceRecords.keySet());
            
            if (attendanceRecords.isEmpty()) {
                log.error("Excel parsing failed. No attendance records found in the file.");
                log.error("Attempting special pattern detection for employee format like '2 : Manjiri Desai'");
                
                // Special handling for files with specific patterns
                try {
                    // Try to extract records with patterns like "2 : Manjiri Desai"
                    attendanceRecords = lookForColonPatternInExcel(file);
                    
                    if (!attendanceRecords.isEmpty()) {
                        log.info("Found {} employees using special colon pattern detection", attendanceRecords.size());
                    } else {
                        log.error("Special pattern detection also failed");
                        SalaryResult errorResult = new SalaryResult();
                        errorResult.setEmployeeId("ERROR");
                        errorResult.setEmployeeName("No valid attendance records found in the Excel file.");
                        return ResponseEntity.ok(Collections.singletonList(errorResult));
                    }
                } catch (Exception e) {
                    log.error("Error in special pattern detection", e);
                    SalaryResult errorResult = new SalaryResult();
                    errorResult.setEmployeeId("ERROR");
                    errorResult.setEmployeeName("No valid attendance records found in the Excel file.");
                    return ResponseEntity.ok(Collections.singletonList(errorResult));
                }
            }
            
            List<SalaryResult> results = salaryComputationService.computeSalaries(attendanceRecords);
            log.info("Computed salary results: {} records", results.size());
            
            // Store results in cache for PDF generation
            lastProcessedResults.clear(); // Clear previous results
            for (SalaryResult result : results) {
                lastProcessedResults.put(result.getEmployeeId(), result);
                log.info("Cached salary result for employee ID: {}", result.getEmployeeId());
            }
            
            if (results.isEmpty()) {
                log.warn("No salary results generated! This could indicate an issue with employee ID mapping or attendance data format");
            } else {
                log.info("First result sample: employeeId={}, name={}, salary={}", 
                    results.get(0).getEmployeeId(),
                    results.get(0).getEmployeeName(),
                    results.get(0).getFinalPayableSalary());
            }
            
            return ResponseEntity.ok(results);
        } catch (Exception e) {
            log.error("Error processing salary data", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Generate PDF salary slips and return as a ZIP file
     * @param file The Excel file with attendance data
     * @return ZIP file with PDF salary slips
     */
    @PostMapping("/generate-pdf")
    public ResponseEntity<byte[]> generatePDFs(@RequestParam("file") MultipartFile file) {
        try {
            log.info("Generating PDF salary slips from file: {}", file.getOriginalFilename());
            
            // Parse the Excel file
            Map<String, List<AttendanceRecord>> attendanceRecords = excelParserService.parseExcel(file);
            
            // Compute salaries
            List<SalaryResult> results = salaryComputationService.computeSalaries(attendanceRecords);
            
            // Generate PDFs
            byte[] zipData = pdfGenerationService.generateSalarySlipsZip(results);
            
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
            String filename = "salary_slips_" + timestamp + ".zip";
            
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .contentLength(zipData.length)
                    .body(zipData);
                    
        } catch (IOException e) {
            log.error("Error generating PDF salary slips", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * Process an Excel file and return both salary results and a ZIP of PDFs
     * @param file The Excel file with attendance data
     * @return JSON with salary results and a Base64 encoded ZIP file
     */
    @PostMapping("/process-with-pdf")
    public ResponseEntity<Map<String, Object>> processSalaryWithPDF(@RequestParam("file") MultipartFile file) {
        try {
            log.info("Processing salary data with PDF generation from file: {}", file.getOriginalFilename());
            
            // Parse the Excel file
            Map<String, List<AttendanceRecord>> attendanceRecords = excelParserService.parseExcel(file);
            
            // Compute salaries
            List<SalaryResult> results = salaryComputationService.computeSalaries(attendanceRecords);
            
            // Generate PDFs
            byte[] zipData = pdfGenerationService.generateSalarySlipsZip(results);
            
            // Return both the salary results and the PDF data
            return ResponseEntity.ok(Map.of(
                    "salaryResults", results,
                    "pdfZipBase64", java.util.Base64.getEncoder().encodeToString(zipData)
            ));
            
        } catch (Exception e) {
            log.error("Error processing salary data with PDF generation", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * Generate a single PDF salary slip for a specific employee
     * @param employeeId The ID of the employee
     * @return PDF salary slip for the employee
     */
    @GetMapping("/pdf/{employeeId}")
    public ResponseEntity<byte[]> generatePDFForEmployee(@PathVariable String employeeId) {
        try {
            log.info("Generating PDF salary slip for employee: {}", employeeId);
            
            // Check if we have cached results for this employee
            if (!lastProcessedResults.containsKey(employeeId)) {
                log.error("No salary results found for employee ID: {}. You must process salary data first.", employeeId);
                return ResponseEntity.notFound().build();
            }
            
            log.info("Found cached salary result for employee ID: {}", employeeId);
            
            // Get the salary result from cache
            SalaryResult employeeResult = lastProcessedResults.get(employeeId);
            
            if (employeeResult == null) {
                log.error("Failed to retrieve salary result for employee ID: {}", employeeId);
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
            }
            
            log.info("Generating PDF for employee: {} ({})", employeeResult.getEmployeeName(), employeeResult.getEmployeeId());
            byte[] pdfData = pdfGenerationService.generateSalarySlip(employeeResult);
            
            String filename = "Salary_Slip_" + employeeId + ".pdf";
            
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                    .contentType(MediaType.APPLICATION_PDF)
                    .contentLength(pdfData.length)
                    .body(pdfData);
                    
        } catch (Exception e) {
            log.error("Error generating PDF for employee: {}", employeeId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * Creates an error result object with the given message
     * @param errorMessage The error message
     * @return SalaryResult object with error information
     */
    private SalaryResult createErrorResult(String errorMessage) {
        SalaryResult errorResult = new SalaryResult();
        errorResult.setEmployeeId("ERROR");
        errorResult.setEmployeeName(errorMessage);
        errorResult.setMonthlySalary(0.0);
        errorResult.setFinalPayableSalary(0.0);
        return errorResult;
    }
    
    /**
     * Creates test attendance records for demonstration purposes when Excel parsing fails
     * @return Map of employee IDs to their attendance records
     */
    private Map<String, List<AttendanceRecord>> createTestAttendanceRecords() {
        Map<String, List<AttendanceRecord>> records = new HashMap<>();
        
        // Create records for EMP001
        List<AttendanceRecord> emp001Records = new ArrayList<>();
        for (int i = 1; i <= 20; i++) {
            AttendanceRecord record = new AttendanceRecord();
            record.setEmployeeId("EMP001");
            record.setDate(LocalDate.of(2025, 6, i));
            record.setStatus(i % 7 == 0 ? "WOP" : "P"); // Weekend every 7th day
            record.setInTime(LocalTime.of(9, 0));
            record.setOutTime(LocalTime.of(17, 0));
            record.setHoursWorked(8.0);
            record.setLate(false);
            emp001Records.add(record);
        }
        records.put("EMP001", emp001Records);
        
        // Create records for EMP002
        List<AttendanceRecord> emp002Records = new ArrayList<>();
        for (int i = 1; i <= 20; i++) {
            AttendanceRecord record = new AttendanceRecord();
            record.setEmployeeId("EMP002");
            record.setDate(LocalDate.of(2025, 6, i));
            if (i % 7 == 0) {
                record.setStatus("WO"); // Weekend
            } else if (i % 10 == 0) {
                record.setStatus("A"); // Absent every 10th day
            } else {
                record.setStatus("P"); // Present
                record.setInTime(LocalTime.of(9, 30));
                record.setOutTime(LocalTime.of(17, 30));
                record.setHoursWorked(8.0);
                record.setLate(false);
            }
            emp002Records.add(record);
        }
        records.put("EMP002", emp002Records);
        
        // Create records for EMP003 (with some late days)
        List<AttendanceRecord> emp003Records = new ArrayList<>();
        for (int i = 1; i <= 20; i++) {
            AttendanceRecord record = new AttendanceRecord();
            record.setEmployeeId("EMP003");
            record.setDate(LocalDate.of(2025, 6, i));
            if (i % 7 == 0) {
                record.setStatus("WO"); // Weekend
            } else {
                record.setStatus("P"); // Present
                // Late 3 days per month
                boolean isLate = (i == 5 || i == 12 || i == 19);
                record.setInTime(LocalTime.of(isLate ? 10 : 9, 15));
                record.setOutTime(LocalTime.of(17, 45));
                record.setHoursWorked(isLate ? 7.5 : 8.5);
                record.setLate(isLate);
            }
            emp003Records.add(record);
        }
        records.put("EMP003", emp003Records);
        
        return records;
    }
}
