#!/bin/bash

# Create a test attendance file in CSV format
cat > /Users/vsalokhe/CascadeProjects/salary-processor-backend/test-files/test_attendance.csv << EOF
Employee ID,Date,In Time,Out Time,Status,Hours Worked
37 : Shravani Shinde,2025-06-01,09:00,18:00,P,9.0
42 - Ananya Patel,2025-06-01,09:30,17:30,P,8.0
Rahul Sharma (56),2025-06-01,10:15,18:15,P,8.0
23 Vijay Kumar,2025-06-01,08:45,17:45,P,9.0
65 : Priya Gupta,2025-06-01,08:30,16:30,P,8.0
78 - Deepak Verma,2025-06-02,09:00,18:00,P,9.0
Neha Singh (92),2025-06-02,09:15,17:15,P,8.0
44 Arjun Reddy,2025-06-02,10:30,19:30,P,9.0
51 : Sanjay Joshi,2025-06-02,08:45,17:45,P,9.0
86 - Ritu Sharma,2025-06-02,09:45,18:45,P,9.0
Amit Patel (19),2025-06-03,08:00,17:00,P,9.0
77 Kavita Gupta,2025-06-03,09:00,18:00,P,9.0
EOF

echo "Test attendance CSV file created at: /Users/vsalokhe/CascadeProjects/salary-processor-backend/test-files/test_attendance.csv"
