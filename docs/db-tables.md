# Database Tables Draft — AP_PROJECT_2025

We use **two separate databases**:
1. **Auth DB** — only authentication and roles
2. **ERP DB** — academic data (students, courses, enrollments, grades, settings)

---

## Auth DB
`users`
- user_id (PK, int, auto_increment)
- username (varchar, unique)
- role (enum: ADMIN, INSTRUCTOR, STUDENT)
- pass_hash (varchar, bcrypt hash)
- status (enum: ACTIVE, LOCKED)
- failed_attempts (int)
- locked_until (datetime, nullable)
- last_login (datetime)

---

## ERP DB
`students`
- user_id (PK, FK → auth.users.user_id)
- roll_no (varchar)
- program (varchar)
- year (int)

`instructors`
- user_id (PK, FK → auth.users.user_id)
- department (varchar)

`courses`
- course_id (PK, auto_increment)
- code (varchar, unique)
- title (varchar)
- credits (int)

`sections`
- section_id (PK, auto_increment)
- course_id (FK → courses.course_id)
- instructor_id (FK → instructors.user_id)
- day_time (varchar)
- room (varchar)
- capacity (int, check >= 0)
- semester (varchar)
- year (int)
- drop_deadline (date)

`enrollments`
- enrollment_id (PK, auto_increment)
- student_id (FK → students.user_id)
- section_id (FK → sections.section_id)
- status (enum: ENROLLED, DROPPED, COMPLETED)
- UNIQUE(student_id, section_id)  ← prevents duplicate enrollment

`grades`
- grade_id (PK, auto_increment)
- enrollment_id (FK → enrollments.enrollment_id)
- component (varchar) e.g. quiz, midterm, final
- score (decimal)
- final_grade (varchar)

`settings`
- key (varchar, PK)
- value (varchar)
- Example seed: ('maintenance','false')

---

✅ Notes:
- **Passwords exist only in Auth DB** (never in ERP DB).
- **Integrity rules**: no duplicate enrollments, capacity ≥ 0, drop deadlines enforced.
