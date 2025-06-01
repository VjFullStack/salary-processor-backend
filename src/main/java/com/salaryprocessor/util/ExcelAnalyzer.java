package com.salaryprocessor.util;

import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.*;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Utility class to analyze Excel file structure
 * This is for development purposes only
 */
public class ExcelAnalyzer {

    public static void main(String[] args) {
        String filePath = "/Users/vsalokhe/CascadeProjects/SalaryProcessor/backend/src/main/resources/WorkDurationReport1.xls";
        
        try (FileInputStream fis = new FileInputStream(filePath);
             Workbook workbook = new HSSFWorkbook(fis)) {
            
            // Get the first sheet
            Sheet sheet = workbook.getSheetAt(0);
            
            // Print sheet name
            System.out.println("Sheet name: " + sheet.getSheetName());
            System.out.println("Total rows: " + sheet.getPhysicalNumberOfRows());
            
            // Get header row
            Row headerRow = sheet.getRow(0);
            if (headerRow != null) {
                System.out.println("\nHeaders:");
                for (int i = 0; i < headerRow.getPhysicalNumberOfCells(); i++) {
                    Cell cell = headerRow.getCell(i);
                    System.out.println(i + ": " + getCellValueAsString(cell));
                }
            }
            
            // Print first few data rows
            System.out.println("\nSample data rows:");
            for (int i = 1; i < Math.min(5, sheet.getPhysicalNumberOfRows()); i++) {
                Row row = sheet.getRow(i);
                if (row != null) {
                    System.out.println("\nRow " + i + ":");
                    for (int j = 0; j < row.getPhysicalNumberOfCells(); j++) {
                        Cell cell = row.getCell(j);
                        System.out.println(j + ": " + getCellValueAsString(cell));
                    }
                }
            }
            
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    private static String getCellValueAsString(Cell cell) {
        if (cell == null) {
            return "null";
        }
        
        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue();
            case NUMERIC:
                if (DateUtil.isCellDateFormatted(cell)) {
                    return cell.getDateCellValue().toString();
                } else {
                    return String.valueOf(cell.getNumericCellValue());
                }
            case BOOLEAN:
                return String.valueOf(cell.getBooleanCellValue());
            case FORMULA:
                return cell.getCellFormula();
            case BLANK:
                return "BLANK";
            default:
                return "UNKNOWN";
        }
    }
}
