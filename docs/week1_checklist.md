# 🗓️ Week 1 — Database & Authentication Setup (Sept 30 – Oct 6)

**Branch:** `feature/db-auth-setup`  
**Goal:** Finalise both databases (auth_db + erp_db), secure seeding (bcrypt), base entities, and UI mockups.  
By the end of Week 1 you should have:
✅ working DDL files  
✅ bcrypt hash generator tool  
✅ verified schema in MySQL  
✅ seed placeholders (no plaintext)  
✅ UI prototypes (LoginFrame,Instructor prototypes,Admin prototypes,Student Prototypes)  
✅ wireframes & docs pushed
✅ ERD DIAGRAM


---

## 📁 FILES TO CREATE / UPDATE

| Area | File | Purpose |
|------|------|----------|
| 🧱 DB | `sql/ddl_auth.sql` | schema for `auth_db` — roles, users (with FK) |
|  | `sql/ddl_erp.sql` | schema for `erp_db` — students, instructors, courses, sections, enrollments, grades, settings |
| 🌱 Seed | `sql/seed_placeholder.sql` | placeholder INSERTs (no real hashes) |
|  | `.gitignore` | must include `sql/seed_real.sql`, `/target`, logs, IDE files |
| 🔐 Tools | `tools/HashPassword.java` | CLI bcrypt generator |
|  | `tools/pom.xml` | small Maven exec plugin for running hash tool |
| ⚙️ Config | `src/main/resources/db.properties.example` | sample DB connection info (no creds) |
| 🔗 Utility | `src/main/java/utils/ConnectionPool.java` | HikariCP connection pool class |
| 🧩 Entities | `src/main/java/edu/univ/erp/domain/Student.java` etc. | skeleton domain models with getters/setters |
| 💻 UI | `src/main/java/edu/univ/erp/ui/LoginFrame.java` | basic login prototype (username + password) |
| 🧾 Docs | `docs/db-tables.md` | short description of every table |
|  | `docs/seed_accounts.md` | how to generate bcrypt hashes + sample command |
|  | `docs/erd-auth-erp.png` | ER diagram (auth_db ↔ erp_db) |
|  | `docs/wireframes/*.png` | login, student, instructor, admin dashboard sketches |
|  | `docs/week1-tasks.md` | this checklist |
|  | `docs/progress.log` | short daily notes (1-2 lines) |

---

## ✅ TASK CHECKLIST

### 1️⃣ Repo & Branch
- [ ] Create repo `AP_PROJECT_2025`
- [ ] `git checkout -b feature/db-auth-setup`
- [ ] Add `.gitignore`, `README.md`, `LICENSE`
- [ ] Verify build: `mvn clean compile`

### 2️⃣ Database DDLs
- [ ] Write `sql/ddl_auth.sql`
- [ ] Write `sql/ddl_erp.sql`
- [ ] Include proper FK constraints and ON UPDATE / ON DELETE rules
- [ ] Run locally:
  ```bash
  mysql -u root -p < sql/ddl_auth.sql
  mysql -u root -p < sql/ddl_erp.sql
