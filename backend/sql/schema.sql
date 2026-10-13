CREATE TABLE IF NOT EXISTS users (
 id BIGINT AUTO_INCREMENT PRIMARY KEY, username VARCHAR(30) NOT NULL UNIQUE,
 password_hash VARCHAR(100) NOT NULL, display_name VARCHAR(50) NOT NULL,
 role VARCHAR(10) NOT NULL, enabled BOOLEAN NOT NULL DEFAULT TRUE,
 CONSTRAINT chk_users_role CHECK (role IN ('ADMIN', 'USER'))
);
CREATE TABLE IF NOT EXISTS equipment_category (
 id BIGINT AUTO_INCREMENT PRIMARY KEY, name VARCHAR(50) NOT NULL UNIQUE
);
CREATE TABLE IF NOT EXISTS equipment (
 id BIGINT AUTO_INCREMENT PRIMARY KEY, name VARCHAR(100) NOT NULL,
 description VARCHAR(500) NOT NULL DEFAULT '', category_id BIGINT NOT NULL,
 total_quantity INT NOT NULL, available_quantity INT NOT NULL, enabled BOOLEAN NOT NULL DEFAULT TRUE,
 CONSTRAINT fk_equipment_category FOREIGN KEY (category_id) REFERENCES equipment_category(id),
 CONSTRAINT chk_equipment_quantity CHECK (total_quantity >= 0 AND available_quantity >= 0 AND available_quantity <= total_quantity)
);
CREATE TABLE IF NOT EXISTS borrow_record (
 id BIGINT AUTO_INCREMENT PRIMARY KEY, user_id BIGINT NOT NULL, equipment_id BIGINT NOT NULL,
 borrow_time DATETIME NOT NULL, return_time DATETIME NULL, status VARCHAR(12) NOT NULL,
 INDEX idx_borrow_user_time (user_id,borrow_time), INDEX idx_borrow_equipment_status (equipment_id,status),
 CONSTRAINT fk_borrow_user FOREIGN KEY (user_id) REFERENCES users(id),
 CONSTRAINT fk_borrow_equipment FOREIGN KEY (equipment_id) REFERENCES equipment(id),
 CONSTRAINT chk_borrow_status CHECK ((status = 'BORROWING' AND return_time IS NULL) OR (status = 'RETURNED' AND return_time IS NOT NULL))
);
