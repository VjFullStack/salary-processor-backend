package com.salaryprocessor.service;

import com.salaryprocessor.model.Employee;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class ContentfulService {
    
    private static final Logger log = LoggerFactory.getLogger(ContentfulService.class);

    // Hardcoded employees with 50,000 INR salary
    private final List<Employee> hardcodedEmployees = new ArrayList<>();
    
    public ContentfulService() {
        // Initialize with some hardcoded employees
        // Create employees using setters instead of constructor
        for (int i = 1; i <= 5; i++) {
            Employee employee = new Employee();
            employee.setEmployeeId(String.format("EMP%03d", i));
            
            String name;
            switch (i) {
                case 1: name = "John Doe"; break;
                case 2: name = "Jane Smith"; break;
                case 3: name = "Mike Johnson"; break;
                case 4: name = "Sarah Williams"; break;
                case 5: name = "David Brown"; break;
                default: name = "Unknown Employee";
            }
            
            employee.setName(name);
            employee.setMonthlySalary(50000.0);
            hardcodedEmployees.add(employee);
        }
    }

    /**
     * Fetch all employees (hardcoded with 50,000 INR salary)
     * @return List of all employees
     */
    public List<Employee> getAllEmployees() {
        log.info("Returning {} hardcoded employees with 50,000 INR salary", hardcodedEmployees.size());
        return new ArrayList<>(hardcodedEmployees);
    }

    /**
     * Fetch an employee by ID (hardcoded with 50,000 INR salary)
     * @param employeeId The ID of the employee to fetch
     * @return The employee with the given ID, or null if not found
     */
    public Employee getEmployeeById(String employeeId) {
        log.info("Fetching hardcoded employee with ID: {}", employeeId);
        return hardcodedEmployees.stream()
                .filter(employee -> employee.getEmployeeId().equals(employeeId))
                .findFirst()
                .orElseGet(() -> {
                    Employee employee = new Employee();
                    employee.setEmployeeId(employeeId);
                    employee.setName("Unknown Employee");
                    employee.setMonthlySalary(50000.0);
                    return employee;
                });
    }

    /**
     * Get a map of employee IDs to employees (hardcoded with 50,000 INR salary)
     * @return Map of employee IDs to employees
     */
    public Map<String, Employee> getEmployeeMap() {
        return hardcodedEmployees.stream()
                .collect(Collectors.toMap(Employee::getEmployeeId, employee -> employee));
    }
    
    /**
     * Creates an employee with the given ID, name, and salary
     * @param id Employee ID
     * @param name Employee name
     * @param salary Monthly salary
     * @return Employee object
     */
    private Employee createEmployee(String id, String name, double salary) {
        Employee employee = new Employee();
        employee.setEmployeeId(id);
        employee.setName(name);
        employee.setMonthlySalary(salary);
        return employee;
    }

    // Add a new employee to our hardcoded list (useful for testing)
    public Employee addEmployee(String id, String name) {
        // Use the createEmployee helper method instead of builder
        Employee newEmployee = createEmployee(id, name, 50000.0); // Always 50,000 INR
        
        hardcodedEmployees.add(newEmployee);
        return newEmployee;
    }
}
