# 🗓️ Week 2 — Authentication + Login + Signup + Session (Oct 7 – 13)

**Branch:** `feature/login`  
**Goal:** Secure login + signup + role routing + session handling.  
By end of this week, app should support:
✅ Student signup (hashed password, stored in both DBs)  
✅ Login with bcrypt verification and 5-try lockout  
✅ Auto-login after signup or via LoginFrame  
✅ Role-based dashboard launch (Admin / Instructor / Student)  
✅ Working unit tests for AuthService + signup flow  

---

## 📁 FILES TO CREATE / UPDATE

| Area | File | Purpose |
|------|------|----------|
| 🔐 Auth Core | `src/main/java/edu/univ/erp/auth/AuthResult.java` | DTO → `boolean success`, `String role`, `long userId`, `String message` |
|  | `src/main/java/edu/univ/erp/auth/AuthService.java` | login + lockout + `registerStudent()` |
| 🧠 Session | `src/main/java/edu/univ/erp/auth/SessionManager.java` | create / get / destroy session |
| 🧾 Signup | `src/main/java/edu/univ/erp/auth/RegisterRequest.java` | DTO for signup data |
|  | `src/main/java/edu/univ/erp/ui/SignUpFrame.java` | Swing signup form |
| 💻 Login UI | `src/main/java/edu/univ/erp/ui/LoginFrame.java` | add “Sign Up” button + AuthService integration |
| 🧪 Tests | `src/test/java/edu/univ/erp/auth/AuthServiceTest.java` | success / fail / lockout |
|  | `src/test/java/edu/univ/erp/auth/RegisterTest.java` | signup success / duplicate / weak pw |
| ⚙️ DAO | `src/main/java/edu/univ/erp/data/UserDAO.java` | add `existsByUsername`, `createUserReturnId`, `deleteById` |
|  | `src/main/java/edu/univ/erp/data/StudentDAO.java` | add `createStudentForUser()` |
| 🧰 Config | `src/main/resources/db.properties` | ensure correct auth_db & erp_db URLs |
| 🗒️ Docs | `docs/week2-tasks.md`, update `docs/progress.log` | mark daily progress |

---

## ✅ TASK CHECKLIST

### 1️⃣ Branch Setup
```bash
git checkout -b feature/login
mvn clean compile
