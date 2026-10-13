-- MySQL 8 演示数据；重复执行仅补充缺失记录，不更新现有密码、资料或库存。
-- 演示登录：admin / Admin123!，student / Student123!。
-- 密码使用 BCrypt（cost 12）哈希，不存储明文密码。
INSERT INTO users(username,password_hash,display_name,role,enabled)
SELECT 'admin','$2a$12$Zd4u/vEGYsNSoSjs371orO88ZYHDmK.Dh3aQVDkAMa1/B0LDtr8bq','系统管理员','ADMIN',TRUE
WHERE NOT EXISTS (SELECT 1 FROM users WHERE username='admin');
INSERT INTO users(username,password_hash,display_name,role,enabled)
SELECT 'student','$2a$12$qCVYOMB2jAFpAVeN9nR2Cu4VqYBigj0sMK.65l2tHCBHQ8plY0dsK','演示学生','USER',TRUE
WHERE NOT EXISTS (SELECT 1 FROM users WHERE username='student');

INSERT INTO equipment_category(name)
SELECT '球类器材' WHERE NOT EXISTS (SELECT 1 FROM equipment_category WHERE name='球类器材');
INSERT INTO equipment_category(name)
SELECT '健身器材' WHERE NOT EXISTS (SELECT 1 FROM equipment_category WHERE name='健身器材');

INSERT INTO equipment(name,description,category_id,total_quantity,available_quantity,enabled)
SELECT '篮球','标准篮球，借用后请妥善保管',id,10,10,TRUE FROM equipment_category
WHERE name='球类器材' AND NOT EXISTS (SELECT 1 FROM equipment WHERE name='篮球');
INSERT INTO equipment(name,description,category_id,total_quantity,available_quantity,enabled)
SELECT '羽毛球拍','每次登记借用1支球拍',id,8,8,TRUE FROM equipment_category
WHERE name='球类器材' AND NOT EXISTS (SELECT 1 FROM equipment WHERE name='羽毛球拍');
INSERT INTO equipment(name,description,category_id,total_quantity,available_quantity,enabled)
SELECT '跳绳','适合日常体能训练',id,15,15,TRUE FROM equipment_category
WHERE name='健身器材' AND NOT EXISTS (SELECT 1 FROM equipment WHERE name='跳绳');
