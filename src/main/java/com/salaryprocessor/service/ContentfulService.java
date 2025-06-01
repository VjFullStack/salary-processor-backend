package com.salaryprocessor.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.salaryprocessor.model.Employee;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class ContentfulService {
    
    private static final Logger log = LoggerFactory.getLogger(ContentfulService.class);
    private static final String EMPLOYEE_CONTENT_TYPE = "employee";
    
    private final RestTemplate restTemplate;
    private final String contentfulBaseUrl;
    private final String contentfulAccessToken;
    private final ObjectMapper objectMapper;
    
    private List<Employee> employeeCache = new ArrayList<>();
    private Map<String, Employee> employeeMapCache = new HashMap<>();
    
    @Autowired
    public ContentfulService(RestTemplate restTemplate, String contentfulBaseUrl, String contentfulAccessToken) {
        this.restTemplate = restTemplate;
        this.contentfulBaseUrl = contentfulBaseUrl;
        this.contentfulAccessToken = contentfulAccessToken;
        this.objectMapper = new ObjectMapper();
        log.info("ContentfulService initialized with baseUrl: {}", contentfulBaseUrl);
    }
    
    /**
     * Initialize the service by fetching data from Contentful
     */
    @PostConstruct
    public void init() {
        log.info("Initializing ContentfulService and fetching initial employee data");
        refreshEmployeeData();
    }

    /**
     * Get all employees from Contentful
     * @return List of Employee objects
     */
    public List<Employee> getAllEmployees() {
        log.info("Getting all employees from cache. Cache size: {}", employeeCache.size());
        return Collections.unmodifiableList(employeeCache);
    }
    
    /**
     * Get an employee by ID from the cache
     * 
     * @param employeeId The employee ID to lookup
     * @return The employee with the given ID, or null if not found
     */
    public Employee getEmployeeById(String employeeId) {
        log.info("Looking up employee by ID: {}", employeeId);
        Employee employee = employeeMapCache.get(employeeId);
        if (employee == null) {
            log.warn("Employee with ID {} not found in cache", employeeId);
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
     * Refresh employee data from Contentful
     */
    public void refreshEmployeeData() {
        log.info("Refreshing employee data from Contentful");
        
        try {
            // Set up HTTP headers with the Content Delivery API token
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + contentfulAccessToken);
            HttpEntity<String> entity = new HttpEntity<>(headers);
            
            // Query Contentful for employee entries
            String url = contentfulBaseUrl + "/entries?content_type=" + EMPLOYEE_CONTENT_TYPE;
            ResponseEntity<String> response = restTemplate.exchange(
                    url, 
                    HttpMethod.GET, 
                    entity, 
                    String.class);
            
            // Parse the JSON response
            JsonNode root = objectMapper.readTree(response.getBody());
            JsonNode items = root.path("items");
            
            log.info("Fetched {} entries from Contentful", items.size());
            
            // Convert Contentful entries to Employee objects
            List<Employee> employees = new ArrayList<>();
            for (JsonNode item : items) {
                Employee employee = mapContentfulEntryToEmployee(item);
                if (employee != null) {
                    employees.add(employee);
                }
            }
            
            // Update cache
            employeeCache = employees;
            employeeMapCache = new HashMap<>();
            for (Employee employee : employees) {
                employeeMapCache.put(employee.getEmployeeId(), employee);
            }
            
            log.info("Updated employee cache with {} employees", employees.size());
            log.info("Employee IDs in cache: {}", employeeMapCache.keySet());
            
        } catch (Exception e) {
            log.error("Error fetching employees from Contentful: {}", e.getMessage(), e);
            // Fall back to default employee data if we can't fetch from Contentful
            createDefaultEmployeeData();
        }
    }
    
    /**
     * Map a Contentful entry to an Employee object
     */
    private Employee mapContentfulEntryToEmployee(JsonNode entry) {
        try {
            Employee employee = new Employee();
            
            // Extract fields from Contentful entry
            JsonNode fields = entry.path("fields");
            
            // Employee ID is required
            if (fields.has("employeeId")) {
                employee.setEmployeeId(fields.path("employeeId").asText());
            } else {
                log.warn("Skipping employee entry without employeeId");
                return null;
            }
            
            // Name is required
            if (fields.has("name")) {
                employee.setName(fields.path("name").asText());
            } else {
                log.warn("Skipping employee entry without name");
                return null;
            }
            
            // Monthly salary
            if (fields.has("monthlySalary")) {
                employee.setMonthlySalary(fields.path("monthlySalary").asDouble());
            } else {
                // Default salary if not specified
                employee.setMonthlySalary(50000.0);
                log.info("Using default salary for employee {}", employee.getEmployeeId());
            }
            
            log.info("Mapped Contentful entry to Employee: ID={}, Name={}, Salary={}", 
                    employee.getEmployeeId(), employee.getName(), employee.getMonthlySalary());
            return employee;
            
        } catch (Exception e) {
            log.error("Error mapping Contentful entry to Employee: {}", e.getMessage(), e);
            return null;
        }
    }
    
    /**
     * Create default employee data as a fallback
     */
    private void createDefaultEmployeeData() {
        log.warn("Creating default employee data as fallback");
        
        List<Employee> defaultEmployees = new ArrayList<>();
        
        // Add some default employees
        Employee emp1 = new Employee();
        emp1.setEmployeeId("EMP001");
        emp1.setName("John Doe");
        emp1.setMonthlySalary(50000.0);
        defaultEmployees.add(emp1);
        
        Employee emp2 = new Employee();
        emp2.setEmployeeId("EMP002");
        emp2.setName("Jane Smith");
        emp2.setMonthlySalary(60000.0);
        defaultEmployees.add(emp2);
        
        // Update cache with default data
        employeeCache = defaultEmployees;
        employeeMapCache = new HashMap<>();
        for (Employee employee : defaultEmployees) {
            employeeMapCache.put(employee.getEmployeeId(), employee);
        }
        
        log.info("Created default employee cache with {} employees", defaultEmployees.size());
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
