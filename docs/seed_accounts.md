# Seed Accounts — AP_PROJECT_2025

We will seed the following demo accounts into Auth DB + ERP DB:

## Auth DB (users)
- admin1 (role = ADMIN)
- inst1 (role = INSTRUCTOR)
- stu1 (role = STUDENT)
- stu2 (role = STUDENT)

## ERP DB (profiles)
- inst1 → Instructor, Department: CSE
- stu1 → Student, Roll: 2024134, Program: BTech CSE, Year: 2
- stu2 → Student, Roll: 2024137, Program: BTech CSE, Year: 2
- Courses: e.g. CSE201 (Advanced Programming), MTH203 (Multivariate Calculus)
- Sections: at least 1 section assigned to inst1
- Enrollments: stu1 enrolled in one section, stu2 in another

## Passwords
- Will use **bcrypt hashes** generated with jBCrypt utility
- Plaintext demo passwords (for video/report only, NOT in DB):
  - admin1 → Admin@123
  - inst1 → Inst@123
  - stu1 → Stu1@123
  - stu2 → Stu2@123

> IMPORTANT: Never commit plaintext passwords into seed SQL.  
> Only insert bcrypt hashes in `sql/seed.sql` using a password-hash generator tool.
