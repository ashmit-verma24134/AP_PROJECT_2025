# DB Tables (Week-1 draft) — AP_PROJECT_2025

We use **two separate databases**:

1. **Auth DB (auth_db)** — *authentication only*: roles and user authentication data.
2. **ERP DB (erp_db)** — academic data: students, instructors, courses, sections, enrollments, grades, settings.

---

## Critical security rule (rubric requirement)
**Passwords (or password equivalents) must only be stored in `auth_db.users.pass_hash` as bcrypt hashes.**  
**ERP DB must never contain plaintext passwords or password hashes.**

---

## Minimal table summary (for reviewer convenience)
**Auth DB**
- `roles` (role_id, role_name)
- `users` (user_id, username, pass_hash, role_id, status, failed_attempts, locked_until, last_login, created_at, updated_at)

**ERP DB**
- `students` (student_id [== auth.users.user_id], roll_no, full_name, program, year, created_at, updated_at)
- `instructors` (instructor_id [== auth.users.user_id], full_name, department, created_at, updated_at)
- `courses` (course_id, code, title, credits, created_at, updated_at)
- `sections` (section_id, course_id → courses, instructor_id, day_time, room, capacity >= 0, semester, year, drop_deadline)
- `enrollments` (enrollment_id, student_id → students, section_id → sections, status, UNIQUE(student_id, section_id))
- `grades` (grade_id, enrollment_id → enrollments, component, score, max_score, final_grade, computed_at)
- `settings` (key, value) — includes `maintenance_on` flag seeded as `'false'`

---

## Notes on linking Auth ↔ ERP
- We **link by convention**: `auth_db.users.user_id` is used as `students.student_id` or `instructors.instructor_id`.  
- Cross-database foreign keys are possible only if both DBs are on the same server and you choose to add explicit cross-DB FKs; for Week-1 it's acceptable to enforce the link by application logic and insertion order.

---

## Integrity & constraints included (meets rubric)
- `UNIQUE(student_id, section_id)` on `enrollments` to prevent duplicate enrollments.
- `CHECK (capacity >= 0)` on `sections.capacity` (MySQL older versions may ignore CHECK; still include it in DDL).
- FK constraints and ON DELETE/ON UPDATE behaviors on ERP tables to preserve integrity between courses → sections → enrollments → grades.
- `settings` table includes maintenance flag (`'maintenance_on'` seeded).

---

## Next steps (recommended)
1. Add migration tooling (Flyway or Liquibase) and store DDL migrations in `sql/migrations/`.
2. Add seed script `sql/seed_filled.sql` that uses bcrypt hashes generated locally (do not commit plaintext passwords).
3. Add indexes based on observed query patterns (e.g., frequent lookups by `roll_no`, `course.code`, `section_id`).
4. Decide whether to enforce Auth↔ERP mapping via cross-DB FK (only if deploying both DBs on same MySQL instance).

---

## Files to review
- `sql/ddl_auth.sql` — Auth DB DDL (this file)
- `sql/ddl_erp.sql` — ERP DB DDL (this file)
- `docs/erd-auth-erp.png` — ER diagram (export from diagrams.net)
