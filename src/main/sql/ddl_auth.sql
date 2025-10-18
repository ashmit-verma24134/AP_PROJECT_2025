CREATE DATABASE IF NOT EXISTS auth_db DEFAULT CHARACTER SET = utf8mb4 DEFAULT COLLATE = utf8mb4_unicode_ci; 
USE auth_db;
CREATE TABLE IF NOT EXISTS roles (
  role_id INT AUTO_INCREMENT PRIMARY KEY,
  role_name VARCHAR(50) NOT NULL UNIQUE
)ENGINE=InnoDB;
CREATE TABLE IF NOT EXISTS users (
  user_id BIGINT AUTO_INCREMENT PRIMARY KEY,
  username VARCHAR(100) NOT NULL UNIQUE,
  pass_hash VARCHAR(255) NOT NULL,
  role_id INT NOT NULL,
  status ENUM('ACTIVE','SUSPENDED','DELETED') NOT NULL DEFAULT 'ACTIVE',
  failed_attempts INT NOT NULL DEFAULT 0,
  locked_until DATETIME NULL,
  last_login DATETIME NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  CONSTRAINT fk_users_role FOREIGN KEY (role_id) REFERENCES roles(role_id)
    ON UPDATE CASCADE ON DELETE RESTRICT
)ENGINE=InnoDB;
INSERT IGNORE INTO roles (role_name) VALUES ('ADMIN'), ('INSTRUCTOR'), ('STUDENT');
-- Insert users one by one so it's obvious and easy to reference later
INSERT IGNORE INTO users (username, pass_hash, role_id) VALUES ('admin1', '<bcrypt-hash-admin1-placeholder>',(SELECT role_id FROM roles WHERE role_name='ADMIN'));
INSERT IGNORE INTO users (username, pass_hash, role_id) VALUES ('inst1', '<bcrypt-hash-inst1-placeholder>', (SELECT role_id FROM roles WHERE role_name='INSTRUCTOR'));
INSERT IGNORE INTO users (username, pass_hash, role_id) VALUES ('stu1', '<bcrypt-hash-stu1-placeholder>', (SELECT role_id FROM roles WHERE role_name='STUDENT'));