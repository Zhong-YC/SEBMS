package com.sebms;

import com.sebms.Models.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class DemoData implements ApplicationRunner {
 private final Store store;
 private final JdbcTemplate jdbc;
 private final boolean enabled;
 public DemoData(Store store,JdbcTemplate jdbc,@Value("${sebms.demo-data:true}") boolean enabled) { this.store=store; this.jdbc=jdbc; this.enabled=enabled; }
 @Override @Transactional public void run(ApplicationArguments args) {
  if(!enabled) return;
  seedUser("admin","Admin123!","系统管理员","ADMIN");
  seedUser("student","Student123!","演示学生","USER");
  jdbc.update("INSERT INTO equipment_category(name) SELECT ? WHERE NOT EXISTS (SELECT 1 FROM equipment_category WHERE name=?)","球类器材","球类器材");
  jdbc.update("INSERT INTO equipment_category(name) SELECT ? WHERE NOT EXISTS (SELECT 1 FROM equipment_category WHERE name=?)","健身器材","健身器材");
  CategoryRow balls=store.categoryByName("球类器材"), fitness=store.categoryByName("健身器材");
  seedEquipment("篮球","标准篮球，借用后请妥善保管",balls.id,10);
  seedEquipment("羽毛球拍","每次登记借用1支球拍",balls.id,8);
  seedEquipment("跳绳","适合日常体能训练",fitness.id,15);
 }
 private void seedUser(String username,String password,String displayName,String role) {
  if(store.userByUsername(username)!=null) return;
  String hash=new BCryptPasswordEncoder(12).encode(password);
  jdbc.update("INSERT INTO users(username,password_hash,display_name,role,enabled) SELECT ?,?,?,?,TRUE WHERE NOT EXISTS (SELECT 1 FROM users WHERE username=?)",username,hash,displayName,role,username);
 }
 private void seedEquipment(String name,String description,long categoryId,int quantity) {
  jdbc.update("INSERT INTO equipment(name,description,category_id,total_quantity,available_quantity,enabled) SELECT ?,?,?,?,?,TRUE WHERE NOT EXISTS (SELECT 1 FROM equipment WHERE name=?)",name,description,categoryId,quantity,quantity,name);
 }
}
