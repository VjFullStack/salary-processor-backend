package com.salaryprocessor.service;

import com.salaryprocessor.model.AttendanceRecord;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class ExcelParserService {

    private static final Logger log = LoggerFactory.getLogger(ExcelParserService.class);

    // Pattern to match "Employee: 123:John Doe" format - capturing only the ID and name
    private static final Pattern EMPLOYEE_PATTERN = Pattern.compile("Employee:\\s*(\\d+)\\s*:\\s*([^\\s].+?)(?=\\s+Total\\s+Work|$)");
    
    // Fallback pattern for direct matching
    private static final Pattern FALLBACK_PATTERN = Pattern.compile("(\\d+)\\s*:\\s*([^\\s].+?)(?=\\s+Total\\s+Work|$)");
    
    // Ultra simple pattern if all else fails
    private static final Pattern SIMPLE_PATTERN = Pattern.compile("Employee:\\s*([^:]+):([^T]+)");
    
    // Patterns for attendance metrics
    private static final Pattern WORK_DURATION_PATTERN = Pattern.compile("Total Work Duration:\\s*([\\d:]+)\\s*Hrs");
    private static final Pattern OT_PATTERN = Pattern.compile("Total OT:\\s*([\\d:]+)\\s*Hrs");
    private static final Pattern PRESENT_PATTERN = Pattern.compile("Present:\\s*([\\d\\.]+)");
    private static final Pattern ABSENT_PATTERN = Pattern.compile("Absent:\\s*([\\d\\.]+)");
    private static final Pattern WEEKLY_OFF_PATTERN = Pattern.compile("WeeklyOff:\\s*([\\d\\.]+)");
    private static final Pattern LATE_HRS_PATTERN = Pattern.compile("Late By Hrs:\\s*([\\d:]+)");
    private static final Pattern LATE_DAYS_PATTERN = Pattern.compile("Late By Days:\\s*([\\d\\.]+)");

    /**
     * Parse Excel file specifically looking for rows with "Employee:" and extract relevant data
     */
    public Map<String, List<AttendanceRecord>> parseExcel(MultipartFile file) {
        log.info("Parsing Excel file: {}", file.getOriginalFilename());
        Map<String, List<AttendanceRecord>> attendanceRecords = new HashMap<>();

        try (InputStream is = file.getInputStream()) {
            Workbook workbook;
            if (file.getOriginalFilename().toLowerCase().endsWith(".xlsx")) {
                workbook = new XSSFWorkbook(is);
            } else {
                workbook = new HSSFWorkbook(is);
            }

            for (int sheetIndex = 0; sheetIndex < workbook.getNumberOfSheets(); sheetIndex++) {
                Sheet sheet = workbook.getSheetAt(sheetIndex);
                log.info("Processing sheet: {}", sheet.getSheetName());

                // Process each row looking for "Employee:"
                for (int rowIndex = 0; rowIndex < sheet.getPhysicalNumberOfRows(); rowIndex++) {
                    Row row = sheet.getRow(rowIndex);
                    if (row == null) continue;

                    String rowContent = getFullRowContent(row);
                    log.debug("Raw row content: {}", rowContent);
                    
                    // Only look for rows containing "Employee:"
                    if (rowContent.contains("Employee:")) {
                        log.info("Found Employee row: {}", rowContent);
                        
                        // Try all patterns one by one
                        String employeeId = null;
                        String employeeName = null;
                        
                        // 1. Try main pattern
                        Matcher employeeMatcher = EMPLOYEE_PATTERN.matcher(rowContent);
                        if (employeeMatcher.find()) {
                            employeeId = employeeMatcher.group(1).trim();
                            employeeName = employeeMatcher.group(2).trim();
                            log.info("Main pattern match: ID={}, Name='{}'", employeeId, employeeName);
                        }
                        
                        // 2. Try fallback pattern if main didn't work
                        if (employeeId == null || employeeName == null) {
                            employeeMatcher = FALLBACK_PATTERN.matcher(rowContent);
                            if (employeeMatcher.find()) {
                                employeeId = employeeMatcher.group(1).trim();
                                employeeName = employeeMatcher.group(2).trim();
                                log.info("Fallback pattern match: ID={}, Name='{}'", employeeId, employeeName);
                            }
                        }
                        
                        // 3. Try simple pattern as last regex attempt
                        if (employeeId == null || employeeName == null) {
                            employeeMatcher = SIMPLE_PATTERN.matcher(rowContent);
                            if (employeeMatcher.find()) {
                                employeeId = employeeMatcher.group(1).trim();
                                employeeName = employeeMatcher.group(2).trim();
                                log.info("Simple pattern match: ID={}, Name='{}'", employeeId, employeeName);
                            }
                        }
                        
                        // 4. Manual extraction as absolute last resort
                        if (employeeId == null || employeeName == null) {
                            // Try to find ID and name by scanning for a number followed by a name
                            if (rowContent.contains("Employee:")) {
                                String afterEmployee = rowContent.substring(rowContent.indexOf("Employee:") + 9).trim();
                                String[] parts = afterEmployee.split(":", 2);
                                if (parts.length == 2) {
                                    employeeId = parts[0].trim();
                                    
                                    // Extract name up to "Total Work Duration" or other metrics
                                    String rawName = parts[1].trim();
                                    int endIndex = rawName.indexOf("Total Work");
                                    if (endIndex > 0) {
                                        employeeName = rawName.substring(0, endIndex).trim();
                                    } else {
                                        employeeName = rawName;
                                    }
                                    
                                    log.info("Manual extraction: ID={}, Name='{}'", employeeId, employeeName);
                                }
                            }
                        }
                        
                        // Clean up any trailing whitespace or punctuation if we found something
                        if (employeeName != null) {
                            employeeName = employeeName.replaceAll("[\\s\\.]+$", "");
                            log.info("Final extracted employee: ID={}, Name='{}'", employeeId, employeeName);
                            
                            // Skip test employee data
                            if (employeeName.toLowerCase().contains("test") || employeeName.equalsIgnoreCase("Employee")) {
                                log.info("Skipping test employee: {}", employeeName);
                                continue;
                            }
                            
                            // Get work hour details from this row and the next row
                            String summaryData = rowContent;
                            
                            // Check if we have a next row to include
                            if (rowIndex + 1 < sheet.getPhysicalNumberOfRows()) {
                                Row nextRow = sheet.getRow(rowIndex + 1);
                                if (nextRow != null) {
                                    String nextRowContent = getFullRowContent(nextRow);
                                    summaryData += " " + nextRowContent;
                                    log.debug("Including next row content: {}", nextRowContent);
                                }
                            }
                            
                            // Extract metrics from the combined summary data
                            double totalWorkHours = extractHoursFromDuration(summaryData, WORK_DURATION_PATTERN);
                            double totalOTHours = extractHoursFromDuration(summaryData, OT_PATTERN);
                            int presentDays = extractNumber(summaryData, PRESENT_PATTERN);
                            int absentDays = extractNumber(summaryData, ABSENT_PATTERN);
                            int weeklyOffDays = extractNumber(summaryData, WEEKLY_OFF_PATTERN);
                            double lateHours = extractHoursFromDuration(summaryData, LATE_HRS_PATTERN);
                            int lateDays = extractNumber(summaryData, LATE_DAYS_PATTERN);
                            
                            log.info("Employee metrics - ID: {}, Name: {}, Work Hours: {}, OT: {}, Present: {}, Absent: {}, WeeklyOff: {}, Late Hrs: {}, Late Days: {}",
                                    employeeId, employeeName, totalWorkHours, totalOTHours, presentDays, 
                                    absentDays, weeklyOffDays, lateHours, lateDays);
                            
                            // Create one attendance record per employee
                            AttendanceRecord record = createAttendanceRecord(
                                    employeeId, employeeName, totalWorkHours, totalOTHours,
                                    presentDays, absentDays, weeklyOffDays, lateHours, lateDays);
                            
                            // Ensure each employee has only one entry in the map
                            attendanceRecords.put(employeeId, Collections.singletonList(record));
                        } else {
                            log.warn("Failed to extract employee data from row: {}", rowContent);
                        }
                    }
                }
            }
            
            workbook.close();
            log.info("Finished parsing. Found {} valid employees.", attendanceRecords.size());
            
        } catch (IOException e) {
            log.error("Error parsing Excel file: {}", e.getMessage(), e);
        }
        
        return attendanceRecords;
    }
    
    /**
     * Create an attendance record with the extracted summary data
     */
    private AttendanceRecord createAttendanceRecord(String employeeId, String employeeName,
                                                  double totalWorkHours, double totalOTHours, 
                                                  int presentDays, int absentDays,
                                                  int weeklyOffDays, double lateHours, int lateDays) {
        
        AttendanceRecord record = new AttendanceRecord();
        record.setEmployeeId(employeeId);
        record.setEmployeeName(employeeName);
        record.setDate(LocalDate.now()); // Current date as reference
        record.setStatus(presentDays > 0 ? "P" : "A"); // Set as Present if there are present days
        record.setHoursWorked(totalWorkHours);
        record.setOvertime(totalOTHours);
        record.setPresentDays(presentDays);
        record.setAbsentDays(absentDays);
        record.setWeeklyOffDays(weeklyOffDays);
        record.setLateHours(lateHours);
        record.setLateDays(lateDays);
        record.setLate(lateDays > 0);
        
        return record;
    }
    
    /**
     * Extract combined text content of all cells in a row
     */
    private String getFullRowContent(Row row) {
        StringBuilder content = new StringBuilder();
        for (int i = 0; i < row.getLastCellNum(); i++) {
            Cell cell = row.getCell(i);
            if (cell != null) {
                content.append(getCellStringValue(cell)).append(" ");
            }
        }
        return content.toString().trim();
    }
    
    /**
     * Extract hours from a duration string like "130:23"
     */
    private double extractHoursFromDuration(String text, Pattern pattern) {
        Matcher matcher = pattern.matcher(text);
        if (matcher.find()) {
            String duration = matcher.group(1).trim();
            log.debug("Found duration: {}", duration);
            String[] parts = duration.split(":");
            try {
                if (parts.length == 2) {
                    int hours = Integer.parseInt(parts[0]);
                    int minutes = Integer.parseInt(parts[1]);
                    
                    // Calculate exactly: HH.MM format (not dividing by 60)
                    // For example: 128:37 should be 128.37, not 128.61666
                    double result = hours;
                    if (minutes > 0) {
                        // Convert minutes to hundredths of an hour (37 minutes = 0.37, not 0.616666)
                        result += minutes / 100.0;
                    }
                    
                    log.debug("Converted duration {}:{} to {} hours", hours, minutes, result);
                    return result;
                }
            } catch (NumberFormatException e) {
                log.warn("Failed to parse duration: {}", duration);
            }
        }
        return 0.0;
    }
    
    /**
     * Extract a numeric value using a regex pattern
     */
    private int extractNumber(String text, Pattern pattern) {
        Matcher matcher = pattern.matcher(text);
        if (matcher.find()) {
            try {
                return Integer.parseInt(matcher.group(1).trim());
            } catch (NumberFormatException e) {
                // Try parsing as a double and convert to int
                try {
                    return (int) Double.parseDouble(matcher.group(1).trim());
                } catch (NumberFormatException ex) {
                    log.warn("Failed to parse number: {}", matcher.group(1));
                }
            }
        }
        return 0;
    }
    
    /**
     * Get string value from a cell, handling different cell types
     */
    private String getCellStringValue(Cell cell) {
        if (cell == null) return "";
        
        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue();
            case NUMERIC:
                if (DateUtil.isCellDateFormatted(cell)) {
                    return cell.getLocalDateTimeCellValue().toString();
                } else {
                    // Handle both integer and decimal numeric values
                    double numericValue = cell.getNumericCellValue();
                    if (numericValue == Math.floor(numericValue)) {
                        return String.valueOf((int) numericValue);
                    } else {
                        return String.valueOf(numericValue);
                    }
                }
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
}
