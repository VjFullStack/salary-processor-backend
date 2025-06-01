package com.salaryprocessor.util;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.FileOutputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * Utility class to generate a test Excel file for testing the Excel parser
 */
public class TestExcelGenerator {
    
    public static void main(String[] args) {
        String filePath = "test-files/test_attendance.xlsx";
        
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Attendance");
            
            // Create header row
            Row headerRow = sheet.createRow(0);
            createCell(headerRow, 0, "Employee ID");
            createCell(headerRow, 1, "Date");
            createCell(headerRow, 2, "In Time");
            createCell(headerRow, 3, "Out Time");
            createCell(headerRow, 4, "Status");
            createCell(headerRow, 5, "Hours Worked");
            
            // Sample data rows
            createDataRow(sheet, 1, "2 : Manjiri Desai", "2025-06-01", "09:00", "18:00", "P", 9.0);
            createDataRow(sheet, 2, "3 : Rahul Sharma", "2025-06-01", "09:30", "17:30", "P", 8.0);
            createDataRow(sheet, 3, "4 : Priya Gupta", "2025-06-01", "10:15", "18:15", "P", 8.0);
            createDataRow(sheet, 4, "5 : Vijay Kumar", "2025-06-01", "08:45", "17:45", "P", 9.0);
            createDataRow(sheet, 5, "6 : Ananya Patel", "2025-06-01", "08:30", "16:30", "P", 8.0);
            
            // Auto-size columns
            for (int i = 0; i < 6; i++) {
                sheet.autoSizeColumn(i);
            }
            
            // Write to file
            try (FileOutputStream fileOut = new FileOutputStream(filePath)) {
                workbook.write(fileOut);
                System.out.println("Test Excel file created successfully at: " + filePath);
            }
            
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    private static void createCell(Row row, int column, String value) {
        Cell cell = row.createCell(column);
        cell.setCellValue(value);
    }
    
    private static void createDataRow(Sheet sheet, int rowNum, String employeeId, String date, 
                                     String inTime, String outTime, String status, double hoursWorked) {
        Row row = sheet.createRow(rowNum);
        createCell(row, 0, employeeId);
        createCell(row, 1, date);
        createCell(row, 2, inTime);
        createCell(row, 3, outTime);
        createCell(row, 4, status);
        
        Cell hoursCell = row.createCell(5);
        hoursCell.setCellValue(hoursWorked);
    }
}
