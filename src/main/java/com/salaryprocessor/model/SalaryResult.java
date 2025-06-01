package com.salaryprocessor.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Represents the result of salary computation for an employee
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SalaryResult {
    private String employeeId;
    private String employeeName;
    private double monthlySalary;
    private double expectedHours;
    private double actualWorkedHours;
    private double coefficient;
    private double finalPayableSalary;
    private int lateMarks;
    private double lateMarkPenalty;
    
    // Explicit getters and setters
    public String getEmployeeId() {
        return employeeId;
    }
    
    public void setEmployeeId(String employeeId) {
        this.employeeId = employeeId;
    }
    
    public String getEmployeeName() {
        return employeeName;
    }
    
    public void setEmployeeName(String employeeName) {
        this.employeeName = employeeName;
    }
    
    public double getMonthlySalary() {
        return monthlySalary;
    }
    
    public void setMonthlySalary(double monthlySalary) {
        this.monthlySalary = monthlySalary;
    }
    
    public double getExpectedHours() {
        return expectedHours;
    }
    
    public void setExpectedHours(double expectedHours) {
        this.expectedHours = expectedHours;
    }
    
    public double getActualWorkedHours() {
        return actualWorkedHours;
    }
    
    public void setActualWorkedHours(double actualWorkedHours) {
        this.actualWorkedHours = actualWorkedHours;
    }
    
    public double getCoefficient() {
        return coefficient;
    }
    
    public void setCoefficient(double coefficient) {
        this.coefficient = coefficient;
    }
    
    public double getFinalPayableSalary() {
        return finalPayableSalary;
    }
    
    public void setFinalPayableSalary(double finalPayableSalary) {
        this.finalPayableSalary = finalPayableSalary;
    }
    
    public int getLateMarks() {
        return lateMarks;
    }
    
    public void setLateMarks(int lateMarks) {
        this.lateMarks = lateMarks;
    }
    
    public double getLateMarkPenalty() {
        return lateMarkPenalty;
    }
    
    public void setLateMarkPenalty(double lateMarkPenalty) {
        this.lateMarkPenalty = lateMarkPenalty;
    }
}
