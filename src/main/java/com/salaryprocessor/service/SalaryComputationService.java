package com.salaryprocessor.service;

import com.salaryprocessor.model.AttendanceRecord;
import com.salaryprocessor.model.Employee;
import com.salaryprocessor.model.SalaryResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.YearMonth;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class SalaryComputationService {
    
    private static final Logger log = LoggerFactory.getLogger(SalaryComputationService.class);

    // Default total working days in a month
    private int totalWorkingDays = 30;
    
    private final ContentfulService contentfulService;
    
    public SalaryComputationService(ContentfulService contentfulService) {
        this.contentfulService = contentfulService;
    }
    
    /**
     * Set the total working days for salary calculations
     * @param days Total working days in the month
     */
    public void setTotalWorkingDays(int days) {
        if (days < 1 || days > 31) {
            log.warn("Invalid total working days value: {}. Using default value of 30.", days);
            this.totalWorkingDays = 30;
        } else {
            log.info("Setting total working days to: {}", days);
            this.totalWorkingDays = days;
        }
    }
    
    /**
     * Get the current total working days setting
     * @return Current total working days value
     */
    public int getTotalWorkingDays() {
        return this.totalWorkingDays;
    }
    
    /**
     * Compute salaries based on attendance data
     * @param attendanceRecords Map of employee IDs to their attendance records
     * @return List of salary computation results
     */
    public List<SalaryResult> computeSalaries(Map<String, List<AttendanceRecord>> attendanceRecords) {
        log.info("Computing salaries for {} employees from attendance records", attendanceRecords.size());
        
        // Initialize list of results
        List<SalaryResult> results = new ArrayList<>();
        
        // Get all employees from Contentful
        List<Employee> employees = contentfulService.getAllEmployees();
        log.info("Retrieved {} employees from Contentful", employees.size());
        log.info("Available employee IDs in Contentful: {}", 
            employees.stream().map(Employee::getEmployeeId).collect(Collectors.toList()));
        
        // Create a map of Employee ID to Employee for easy lookup
        Map<String, Employee> employeeMap = employees.stream()
                .collect(Collectors.toMap(Employee::getEmployeeId, e -> e));
        
        // Find matching employee IDs between attendance records and Contentful
        Set<String> matchingEmployeeIds = new HashSet<>(attendanceRecords.keySet());
        matchingEmployeeIds.retainAll(employeeMap.keySet());
        log.info("Found {} matching employee IDs between attendance records and Contentful: {}", 
            matchingEmployeeIds.size(), matchingEmployeeIds);
        
        // Extract employee names from attendance records
        Map<String, String> extractedNames = new HashMap<>();
        for (Map.Entry<String, List<AttendanceRecord>> entry : attendanceRecords.entrySet()) {
            String empId = entry.getKey();
            List<AttendanceRecord> records = entry.getValue();
            
            // If we have records with employee names, use the first one
            if (!records.isEmpty() && records.get(0).getEmployeeName() != null && !records.get(0).getEmployeeName().isEmpty()) {
                extractedNames.put(empId, records.get(0).getEmployeeName());
                log.info("Extracted name for employee ID {}: {}", empId, records.get(0).getEmployeeName());
            }
        }
        
        // Create default employee map to ensure all employees in attendance records have a name
        Map<String, Employee> defaultEmployeeMap = new HashMap<>();
        for (String empId : attendanceRecords.keySet()) {
            if (!employeeMap.containsKey(empId)) {
                Employee defaultEmployee = new Employee();
                defaultEmployee.setEmployeeId(empId);
                // Use extracted name if available, otherwise use default
                if (extractedNames.containsKey(empId)) {
                    defaultEmployee.setName(extractedNames.get(empId));
                    log.info("Using extracted name for employee ID {}: {}", empId, extractedNames.get(empId));
                } else {
                    defaultEmployee.setName("Employee " + empId);
                    log.info("No extracted name available, using default for ID: {}", empId);
                }
                defaultEmployee.setMonthlySalary(50000.0); // Default salary
                defaultEmployeeMap.put(empId, defaultEmployee);
                log.info("Created default employee for ID: {}", empId);
            }
        }
        
        // Combine the real employee map with default employee map
        for (Map.Entry<String, Employee> entry : defaultEmployeeMap.entrySet()) {
            employeeMap.put(entry.getKey(), entry.getValue());
        }
        
        // Process each employee's attendance records
        attendanceRecords.forEach((employeeId, records) -> {
            log.info("Processing employee ID: {} with {} attendance records", employeeId, records.size());
            Employee employee = employeeMap.get(employeeId);
            
            if (employee == null) {
                log.warn("Employee with ID {} not found in Contentful. Using placeholder employee.", employeeId);
                // This shouldn't happen with our default employee map, but just in case
                employee = new Employee();
                employee.setEmployeeId(employeeId);
                employee.setName("Unknown Employee " + employeeId);
                employee.setMonthlySalary(50000.0); // Default salary
            } else {
                log.info("Found employee in Contentful: ID={}, Name={}", employee.getEmployeeId(), employee.getName());
            }
            
            // Count different attendance statuses
            long presentCount = records.stream()
                    .filter(r -> "P".equalsIgnoreCase(r.getStatus()))
                    .count();
            
            long absentCount = records.stream()
                    .filter(r -> "A".equalsIgnoreCase(r.getStatus()))
                    .count();
            
            long weekOffPresentCount = records.stream()
                    .filter(r -> "WOP".equalsIgnoreCase(r.getStatus()))
                    .count();
            
            // We're ignoring WO (Week Off) records as per requirements
            
            // Count paid leaves (assuming all absences are paid leaves for simplicity)
            long paidLeaves = absentCount;
            
            // Calculate expected hours: (WOP + P + A - paidLeaves) × 8
            double expectedHours = (weekOffPresentCount + presentCount + absentCount - paidLeaves) * 8;
            
            // Calculate worked hours
            // We'll calculate this directly from the record below            
            // This line is now obsolete since we use actualHours from the record directly
            
            // Calculate late marks
            long lateMarks = records.stream()
                    .filter(AttendanceRecord::isLate)
                    .count();
            
            // Get the actual hours worked and overtime from Excel data
            double regularHours = records.isEmpty() ? 0 : records.get(0).getHoursWorked();
            double overtimeHours = records.isEmpty() ? 0 : records.get(0).getOvertime();
            
            // Calculate total actual hours (regular + overtime)
            double actualHours = regularHours + overtimeHours;
            
            log.info("Employee {} hours calculation: Regular Hours={}, Overtime={}, Total Hours={}", 
                     employeeId, regularHours, overtimeHours, actualHours);
            
            // Use the standard working hours - typically 8 hours per day
            double expectedHoursPerDay = 8.0;
            
            // Full month standard hours (totalWorkingDays × 8 hours)
            double fullMonthHours = totalWorkingDays * expectedHoursPerDay;
            
            log.info("Using totalWorkingDays={} for employee {} calculation, expected hours={}", 
                     totalWorkingDays, employeeId, fullMonthHours);
            
            // Calculate ratio based on Excel data - actual hours worked / standard month hours
            double workRatio = actualHours / fullMonthHours;
            
            // If worked more than expected, don't cap at 100%
            // double adjustedRatio = Math.min(1.0, workRatio); // Old code with cap
            double adjustedRatio = workRatio; // Allow ratios above 1.0 if overtime exceeds expectations
            
            // Calculate percentage for display
            double workPercentage = adjustedRatio * 100;
            
            // Round to 2 decimal places for clean display
            workPercentage = Math.round(workPercentage * 100) / 100.0;
            
            log.info("Employee {} work ratio from Excel: {} hours / {} expected = {}% (ratio: {})", 
                     employeeId, actualHours, fullMonthHours, workPercentage, adjustedRatio);
            
            // Calculate final salary based on work ratio from Excel data
            double finalSalary = employee.getMonthlySalary() * adjustedRatio;
            
            // Apply any late mark penalties based on business rules
            double lateMarkPenalty = 0;
            if (lateMarks > 0) {
                // Calculate daily salary based on monthly salary
                // Always use 30 days as per business requirement
                double dailySalary = employee.getMonthlySalary() / 30;
                double halfDaySalary = dailySalary / 2;
                
                // Business rule: First two late marks are forgiven
                // Third late mark incurs half day salary deduction
                // Each additional late mark: (half day's salary / 3)
                if (lateMarks < 3) {
                    // First two late marks are forgiven
                    lateMarkPenalty = 0;
                    log.info("First two late marks forgiven, no penalty applied");
                } else if (lateMarks == 3) {
                    // Only third late mark incurs half day salary deduction
                    lateMarkPenalty = halfDaySalary;
                } else {
                    // Third late mark: half day's salary
                    // Each additional late mark after third: (half day's salary / 3)
                    long additionalLateMarks = lateMarks - 3;
                    double additionalPenalty = (halfDaySalary / 3) * additionalLateMarks;
                    lateMarkPenalty = halfDaySalary + additionalPenalty;
                }
                
                finalSalary -= lateMarkPenalty;
                log.info("Applied late mark penalty of {} for {} late marks (half day salary: {})", 
                         lateMarkPenalty, lateMarks, halfDaySalary);
            }
            
            log.info("Calculating final salary: base={}, work ratio={}%, final={}", 
                     employee.getMonthlySalary(), workPercentage, finalSalary);
            
            // Add to results
            SalaryResult result = new SalaryResult();
            result.setEmployeeId(employeeId);
            result.setEmployeeName(employee.getName());
            result.setMonthlySalary(employee.getMonthlySalary());
            result.setExpectedHours(fullMonthHours); // Total expected hours (days * 8)
            result.setActualWorkedHours(actualHours);     // From Excel data
            
            // Set coefficient as the actual ratio between worked hours and expected hours
            // For 149.2 hours worked in a standard 240h month (30 days × 8h), coefficient would be 0.6217 or 62.17%
            result.setCoefficient(adjustedRatio);
            
            // Late marks affect final salary but aren't part of the coefficient display
            result.setLateMarkPenalty(lateMarkPenalty);
            result.setLateMarks((int) lateMarks);
            result.setFinalPayableSalary(finalSalary);
            results.add(result);
            
            log.info("Calculated salary for employee {}: monthly={}, final={}, work ratio={}%", 
                    employeeId, employee.getMonthlySalary(), finalSalary, workPercentage);
        });
        
        return results;
    }
    
    /**
     * Calculate coefficient based on attendance and late marks
     * 
     * @param presentCount Number of present days
     * @param absentCount Number of absent days
     * @param weekOffPresentCount Number of week-off days worked
     * @param lateMarks Number of late marks
     * @param totalWorkingDays Total number of working days in the month
     * @return Performance coefficient for salary calculation
     */
    private double calculateCoefficient(long presentCount, long absentCount, long weekOffPresentCount, long lateMarks, int totalWorkingDays) {
        // For Excel data compatibility, return the coefficient directly from the attendance data
        // Parse coefficient from Excel data - use presentDays / expectedDays
        double expectedDays = totalWorkingDays;
        double actualDays = presentCount + weekOffPresentCount;
        
        // Calculate coefficient (actual work / expected work)
        double coefficient = actualDays / expectedDays;
        
        // Convert to percentage representation for display
        log.debug("Raw coefficient calculation: {} days / {} days = {}", actualDays, expectedDays, coefficient);
        
        // Round to 2 decimal places for display (this won't affect calculations)
        coefficient = Math.round(coefficient * 100) / 100.0;
        
        return coefficient;
    }
    
    /**
     * Calculate late mark penalty based on the business rules:
     * - First 3 late marks = 0.5 day salary cut (0.5/30 = 0.01667 coefficient reduction)
     * - From 4th onward: 0.5/3 day cut per late mark (0.5/3/30 = 0.00556 coefficient reduction per mark)
     * 
     * @param lateMarks Number of late marks
     * @return Penalty as a coefficient reduction
     */
    private double calculateLateMarkPenalty(long lateMarks) {
        if (lateMarks <= 0) {
            return 0;
        }
        
        // Assuming 30 days in a month
        double penaltyPerDay = 1.0 / 30.0;
        
        if (lateMarks <= 3) {
            // First 3 late marks = 0.5 day salary cut
            return 0.5 * penaltyPerDay;
        } else {
            // First 3 late marks = 0.5 day salary cut
            // Each additional mark = 0.5/3 day salary cut
            return 0.5 * penaltyPerDay + ((lateMarks - 3) * 0.5 / 3.0) * penaltyPerDay;
        }
    }
}
