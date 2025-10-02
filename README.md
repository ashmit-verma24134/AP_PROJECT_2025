# University ERP System (AP Project 2025)

## Overview
A Java-based ERP system for university management, built with Swing (UI) and MySQL (DB).  
Includes role-based dashboards (Student, Instructor, Admin), authentication, course enrollment, and reporting features.

---

## Week 1 Objectives
- Establish clean project skeleton with Maven.
- Define package structure (`edu.univ.erp.*`).
- Draft ER diagram and database schema (Auth DB + ERP DB).
- Create wireframes for login + dashboards.
- Decide team plan (roles, branch strategy, meeting schedule).
- Document seed-data plan.

---

## Deliverables (Week 1)
- ✅ GitHub repo with initial Maven commit.
- ✅ `.gitignore`, `LICENSE`, `README.md`.
- ✅ Package skeleton with placeholder classes.
- ✅ Draft DB schema & ER diagram.
- ✅ Wireframes: login + dashboards.
- ✅ Team plan (roles, comm channel, meeting schedule).
- ✅ Week-1 checklist mapping to rubric items.

---

## Mapping to Rubric
- **Code / Project organization (10 pts)** → Package skeleton + `README.md`.
- **Authentication separation (10 pts)** → Auth DB schema separate from ERP DB.
- **Data design & integrity (10 pts)** → ER diagram + DDL with constraints.
- **UI/UX quality (10 pts)** → Wireframes + login prototype.
- **Documentation & readiness** → Checklist, decisions, and seed-data plan.

---

## How to Run (placeholder)
```bash
# build
mvn package
# run prototype
java -cp target/univ-erp-1.0-SNAPSHOT.jar edu.univ.erp.Main
