USE auth_db;
INSERT IGNORE INTO roles(role_name) VALUES ('ADMIN'), ('INSTRUCTOR'), ('STUDENT');
INSERT INTO users(username, pass_hash, role_id) VALUES
('admin1', '<bcrypt-hash-admin1>', (SELECT role_id FROM roles WHERE role_name='ADMIN')),
('inst1', '<bcrypt-hash-inst1>', (SELECT role_id FROM roles WHERE role_name='INSTRUCTOR')),
('stu1',  '<bcrypt-hash-stu1>',  (SELECT role_id FROM roles WHERE role_name='STUDENT'));
USE erp_db;
INSERT INTO courses(code, title, credits) VALUES ('CS101','Intro to Programming',3.0);
-- Example student/instructor (replace IDs with actual user_id values from auth_db.users)
-- INSERT INTO students(student_id, roll_no, full_name, program, year) 
-- VALUES (/*stu1 user_id*/, '2024134', 'Ashmit Verma', 'BTech CSD', 2);
-- INSERT INTO instructors(instructor_id, full_name, department) 
-- VALUES (/*inst1 user_id*/, 'Dr. Example', 'CSE');
--HERE WE planned our seed users securely, without storing real passwords.Instead we convert that password in to bcrypt hashed form so that passwords store securely....
"C:\Program Files\MySQL\MySQL Server 8.0\bin\mysql.exe" -u root -p < src\main\sql\ddl_auth.sql
"C:\Program Files\MySQL\MySQL Server 8.0\bin\mysql.exe" -u root -p < src\main\resources\sql\ddl_erp.sql
"C:\Program Files\MySQL\MySQL Server 8.0\bin\mysql.exe" -u root -p < src\main\resources\sql\seed_placeholder.sql
