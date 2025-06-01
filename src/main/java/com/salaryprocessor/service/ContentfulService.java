package com.salaryprocessor.service;

import com.contentful.java.cda.CDAClient;
import com.contentful.java.cda.CDAEntry;
import com.contentful.java.cda.CDAArray;
import com.contentful.java.cda.CDAResource;
import com.salaryprocessor.model.Employee;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class ContentfulService {
    
    private static final Logger log = LoggerFactory.getLogger(ContentfulService.class);
    private static final String EMPLOYEE_CONTENT_TYPE = "employee";
    
    private final CDAClient contentfulClient;
    private List<Employee> employeeCache = new ArrayList<>();
    private Map<String, Employee> employeeMapCache = new HashMap<>();
    
    public ContentfulService(CDAClient contentfulClient) {
        this.contentfulClient = contentfulClient;
    }
    
    /**
     * Initialize the service by fetching data from Contentful
     */
    @PostConstruct
    public void init() {
        refreshEmployeeData();
    }

    /**
     * Refreshes employee data from Contentful
     */
    public void refreshEmployeeData() {
        try {
            log.info("Fetching employee data from Contentful");
            CDAArray entries = contentfulClient.fetch(CDAEntry.class)
                    .withContentType(EMPLOYEE_CONTENT_TYPE)
                    .all();
            
            if (entries != null) {
                List<Employee> employees = new ArrayList<>();
                Map<String, Employee> employeeMap = new HashMap<>();
                
                for (CDAResource resource : entries.items()) {
                    if (resource instanceof CDAEntry) {
                        CDAEntry entry = (CDAEntry) resource;
                        Employee employee = convertEntryToEmployee(entry);
                        employees.add(employee);
                        employeeMap.put(employee.getEmployeeId(), employee);
                    }
                }
                
                this.employeeCache = employees;
                this.employeeMapCache = employeeMap;
                log.info("Successfully loaded {} employees from Contentful", employees.size());
            } else {
                log.warn("No entries returned from Contentful");
            }
        } catch (Exception e) {
            log.error("Error fetching employee data from Contentful: {}", e.getMessage(), e);
            // If fetch fails, we'll still have the cached data (or empty lists if first time)
        }
    }
    
    /**
     * Converts a Contentful Entry to an Employee object
     */
    private Employee convertEntryToEmployee(CDAEntry entry) {
        String employeeId = entry.getField("employeeId");
        String name = entry.getField("name");
        Double monthlySalary = entry.getField("monthlySalary");
        
        log.debug("Converting Contentful entry to Employee: id={}, name={}, salary={}", 
                employeeId, name, monthlySalary);
        
        Employee employee = new Employee();
        employee.setEmployeeId(employeeId);
        employee.setName(name);
        employee.setMonthlySalary(monthlySalary != null ? monthlySalary : 0.0);
        return employee;
    }
    
    /**
     * Fetch all employees from Contentful
     * @return List of all employees
     */
    public List<Employee> getAllEmployees() {
        if (employeeCache.isEmpty()) {
            refreshEmployeeData();
        }
        log.info("Returning {} employees from Contentful cache", employeeCache.size());
        return new ArrayList<>(employeeCache);
    }

    /**
     * Fetch an employee by ID from Contentful
     * @param employeeId The ID of the employee to fetch
     * @return The employee with the given ID, or a default employee if not found
     */
    public Employee getEmployeeById(String employeeId) {
        if (employeeMapCache.isEmpty()) {
            refreshEmployeeData();
        }
        
        log.info("Fetching employee with ID: {}", employeeId);
        Employee employee = employeeMapCache.get(employeeId);
        
        if (employee == null) {
            log.warn("Employee with ID {} not found in Contentful, creating default employee", employeeId);
            employee = createDefaultEmployee(employeeId);
        }
        
        return employee;
    }

    /**
     * Get a map of employee IDs to employees from Contentful
     * @return Map of employee IDs to employees
     */
    public Map<String, Employee> getEmployeeMap() {
        if (employeeMapCache.isEmpty()) {
            refreshEmployeeData();
        }
        return new HashMap<>(employeeMapCache);
    }
    
    /**
     * Create a default employee with the given ID
     * Used as a fallback when an employee is not found in Contentful
     */
    private Employee createDefaultEmployee(String employeeId) {
        Employee employee = new Employee();
        employee.setEmployeeId(employeeId);
        employee.setName("Unknown Employee");
        employee.setMonthlySalary(50000.0); // Default salary
        return employee;
    }
}
