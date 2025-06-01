package com.salaryprocessor.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AttendanceRecord {
    private String employeeId;
    private String employeeName;
    private LocalDate date;
    private LocalTime inTime;
    private LocalTime outTime;
    private String status; // WO (Week Off), P (Present), A (Absent), WOP (Week Off Present)
    private double hoursWorked;
    private boolean isLate;
    
    // New fields for summary data
    private double overtime;
    private int presentDays;
    private int absentDays;
    private int weeklyOffDays;
    private double lateHours;
    private int lateDays;
    
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
    
    public LocalDate getDate() {
        return date;
    }
    
    public void setDate(LocalDate date) {
        this.date = date;
    }
    
    public LocalTime getInTime() {
        return inTime;
    }
    
    public void setInTime(LocalTime inTime) {
        this.inTime = inTime;
    }
    
    public LocalTime getOutTime() {
        return outTime;
    }
    
    public void setOutTime(LocalTime outTime) {
        this.outTime = outTime;
    }
    
    public String getStatus() {
        return status;
    }
    
    public void setStatus(String status) {
        this.status = status;
    }
    
    public double getHoursWorked() {
        return hoursWorked;
    }
    
    public void setHoursWorked(double hoursWorked) {
        this.hoursWorked = hoursWorked;
    }
    
    public boolean isLate() {
        return isLate;
    }
    
    public void setLate(boolean late) {
        isLate = late;
    }
    
    public double getOvertime() {
        return overtime;
    }
    
    public void setOvertime(double overtime) {
        this.overtime = overtime;
    }
    
    public int getPresentDays() {
        return presentDays;
    }
    
    public void setPresentDays(int presentDays) {
        this.presentDays = presentDays;
    }
    
    public int getAbsentDays() {
        return absentDays;
    }
    
    public void setAbsentDays(int absentDays) {
        this.absentDays = absentDays;
    }
    
    public int getWeeklyOffDays() {
        return weeklyOffDays;
    }
    
    public void setWeeklyOffDays(int weeklyOffDays) {
        this.weeklyOffDays = weeklyOffDays;
    }
    
    public double getLateHours() {
        return lateHours;
    }
    
    public void setLateHours(double lateHours) {
        this.lateHours = lateHours;
    }
    
    public int getLateDays() {
        return lateDays;
    }
    
    public void setLateDays(int lateDays) {
        this.lateDays = lateDays;
    }
}
