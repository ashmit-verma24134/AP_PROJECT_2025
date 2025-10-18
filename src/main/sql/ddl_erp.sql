CREATE DATABASE IF NOT EXISTS erp_db DEFAULT CHARACTER SET = utf8mb4 DEFAULT COLLATE = utf8mb4_unicode_ci;
USE erp_db;
CREATE TABLE IF NOT EXISTS students(
  student_id BIGINT PRIMARY KEY,                 -- will match auth_db.users.user_id  (mentioned in erd diagram)
  roll_no VARCHAR(50) NOT NULL UNIQUE,
  full_name VARCHAR(200) NOT NULL,
  program VARCHAR(100),
  year INT,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
)ENGINE=InnoDB;
CREATE TABLE IF NOT EXISTS instructors(
  instructor_id BIGINT PRIMARY KEY,              -- will match auth_db.users.user_id (mentioned in erd diagram :/)
  full_name VARCHAR(200) NOT NULL,
  department VARCHAR(100),
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
)ENGINE=InnoDB;
CREATE TABLE IF NOT EXISTS courses(
  course_id BIGINT AUTO_INCREMENT PRIMARY KEY,
  code VARCHAR(50) NOT NULL UNIQUE,
  title VARCHAR(200) NOT NULL,
  credits DECIMAL(3,1) NOT NULL DEFAULT 4.0,
  mode ENUM('REGULAR', 'ONLINE', 'MINI') NOT NULL DEFAULT 'REGULAR',
  is_pass_fail TINYINT(1) NOT NULL DEFAULT 0,
  grading_scheme ENUM('NUMERIC', 'LETTER', 'PASSFAIL') NOT NULL DEFAULT 'LETTER',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
)ENGINE=InnoDB;
CREATE TABLE IF NOT EXISTS sections(
  section_id BIGINT AUTO_INCREMENT PRIMARY KEY,
  course_id BIGINT NOT NULL,
  instructor_id BIGINT NULL,
  day_time VARCHAR(100),
  room VARCHAR(100),
  capacity INT NOT NULL DEFAULT 30 CHECK (capacity>=0),
  semester VARCHAR(20),
  year INT,
  drop_deadline DATE,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  FOREIGN KEY (course_id) REFERENCES courses(course_id) ON UPDATE CASCADE ON DELETE RESTRICT,
  FOREIGN KEY (instructor_id) REFERENCES instructors(instructor_id) ON UPDATE CASCADE ON DELETE SET NULL
)ENGINE=InnoDB;
CREATE TABLE IF NOT EXISTS enrollments(
  enrollment_id BIGINT AUTO_INCREMENT PRIMARY KEY,
  student_id BIGINT NOT NULL,
  section_id BIGINT NOT NULL,
  status ENUM('ENROLLED','DROPPED','WAITLISTED','COMPLETED') NOT NULL DEFAULT 'ENROLLED',
  enrolled_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE (student_id, section_id),
  FOREIGN KEY (student_id) REFERENCES students(student_id) ON UPDATE CASCADE ON DELETE CASCADE,
  FOREIGN KEY (section_id) REFERENCES sections(section_id) ON UPDATE CASCADE ON DELETE CASCADE
)ENGINE=InnoDB;
CREATE TABLE IF NOT EXISTS grades(
  grade_id BIGINT AUTO_INCREMENT PRIMARY KEY,
  enrollment_id BIGINT NOT NULL,
  component VARCHAR(100) NOT NULL DEFAULT 'FINAL',
  letter_grade ENUM(
    'A+','A','A-',
    'B','B-',
    'C','C-',
    'D','F',
    'I','W',
    'S','X'
  )NOT NULL,
  points DECIMAL(4,2) GENERATED ALWAYS AS (
    CASE
      WHEN letter_grade IN ('A+','A') THEN 10
      WHEN letter_grade = 'A-' THEN 9
      WHEN letter_grade = 'B' THEN 8
      WHEN letter_grade = 'B-' THEN 7
      WHEN letter_grade = 'C' THEN 6
      WHEN letter_grade = 'C-' THEN 5
      WHEN letter_grade = 'D' THEN 4
      WHEN letter_grade = 'F' THEN 2
      ELSE 0
    END
  )STORED,
  remarks VARCHAR(200) NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (enrollment_id) REFERENCES enrollments(enrollment_id) ON UPDATE CASCADE ON DELETE CASCADE
)ENGINE=InnoDB;
CREATE TABLE IF NOT EXISTS settings(
  `key` VARCHAR(100) PRIMARY KEY,
  `value` VARCHAR(500),
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB;
INSERT INTO settings (`key`, `value`)
VALUES 
('maintenance_on', 'false'),
('max_cg', '10'),
('min_pass_points', '4'),
('default_course_credits', '4'),
('f_points', '2')
ON DUPLICATE KEY UPDATE `value` = VALUES(`value`);
INSERT IGNORE INTO courses (code, title, credits, mode, grading_scheme) VALUES
('CS101', 'Intro to Programming', 4.0, 'REGULAR', 'LETTER'),
('CS200M', 'Mini Course: Advanced Git Workshop', 2.0, 'MINI', 'LETTER'),
('CS301O', 'Intro to Cloud (Online)', 4.0, 'ONLINE', 'PASSFAIL');
-- 3) Insert students / instructors referencing auth_db.users
-- Use subqueries to set student_id/instructor_id to the correct auth_db.users.user_id
INSERT INTO erp_db.students (student_id, roll_no, full_name, program, year)
VALUES (
  (SELECT user_id FROM auth_db.users WHERE username='stu1'),
  'R001', 'Student One', 'B.Tech CSE', 2
);
INSERT INTO erp_db.instructors (instructor_id, full_name, department)
VALUES (
  (SELECT user_id FROM auth_db.users WHERE username='inst1'),
  'Instructor One', 'CSE'
);
-- 4) Add cross-database FKs (do after the above inserts)
ALTER TABLE erp_db.students ADD CONSTRAINT fk_students_user FOREIGN KEY (student_id) REFERENCES auth_db.users(user_id) ON UPDATE CASCADE ON DELETE RESTRICT;
ALTER TABLE erp_db.instructors ADD CONSTRAINT fk_instructors_user FOREIGN KEY (instructor_id) REFERENCES auth_db.users(user_id) ON UPDATE CASCADE ON DELETE RESTRICT;
-- 5) Views (run last)
CREATE OR REPLACE VIEW erp_db.v_student_performance AS
SELECT  s.student_id,  s.full_name, sec.semester, sec.year, ROUND(SUM(g.points * c.credits) / NULLIF(SUM(c.credits),0), 2) AS sgpa FROM erp_db.grades g JOIN erp_db.enrollments e ON g.enrollment_id = e.enrollment_id JOIN erp_db.sections sec ON e.section_id = sec.section_id JOIN erp_db.courses c ON sec.course_id = c.course_id JOIN erp_db.students s ON e.student_id = s.student_id WHERE g.letter_grade NOT IN ('I', 'W', 'S', 'X')
GROUP BY s.student_id, sec.semester, sec.year;
CREATE OR REPLACE VIEW erp_db.v_student_cgpa AS SELECT  s.student_id, s.full_name,ROUND(SUM(g.points * c.credits) / NULLIF(SUM(c.credits),0), 2) AS cgpa FROM erp_db.grades g JOIN erp_db.enrollments e ON g.enrollment_id = e.enrollment_id JOIN erp_db.sections sec ON e.section_id = sec.section_id JOIN erp_db.courses c ON sec.course_id = c.course_id JOIN erp_db.students s ON e.student_id = s.student_id WHERE g.letter_grade NOT IN ('I', 'W', 'S', 'X') GROUP BY s.student_id;