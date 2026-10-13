package com.sebms;

import com.sebms.Models.*;
import java.time.LocalDateTime;
import java.util.List;
import org.apache.ibatis.annotations.*;

@Mapper
public interface Store {
 String EQUIPMENT_VIEW = "SELECT e.*, c.name category_name FROM equipment e JOIN equipment_category c ON c.id=e.category_id";
 String BORROW_VIEW = "SELECT b.*,u.username,u.display_name,e.name equipment_name FROM borrow_record b JOIN users u ON u.id=b.user_id JOIN equipment e ON e.id=b.equipment_id";

 @Select("SELECT * FROM users WHERE username=#{username}") UserRow userByUsername(String username);
 @Select("SELECT * FROM users WHERE id=#{id}") UserRow user(long id);
 @Select("SELECT * FROM users WHERE id=#{id} FOR UPDATE") UserRow lockUser(long id);
 @Select("SELECT * FROM users ORDER BY id") List<UserRow> users();
 @Select("SELECT * FROM users WHERE role='ADMIN' ORDER BY id FOR UPDATE") List<UserRow> lockAdmins();
 @Insert("INSERT INTO users(username,password_hash,display_name,role,enabled) VALUES(#{username},#{passwordHash},#{displayName},#{role},#{enabled})")
 @Options(useGeneratedKeys=true,keyProperty="id") int insertUser(UserRow user);
 @Update("UPDATE users SET display_name=#{displayName},role=#{role},enabled=#{enabled},password_hash=#{passwordHash} WHERE id=#{id}") int updateUser(UserRow user);
 @Delete("DELETE FROM users WHERE id=#{id}") int deleteUser(long id);
 @Select("SELECT COUNT(*) FROM borrow_record WHERE user_id=#{id}") long userHistory(long id);

 @Select("SELECT * FROM equipment_category ORDER BY id") List<CategoryRow> categories();
 @Select("SELECT * FROM equipment_category WHERE id=#{id} FOR UPDATE") CategoryRow lockCategory(long id);
 @Select("SELECT * FROM equipment_category WHERE name=#{name}") CategoryRow categoryByName(String name);
 @Insert("INSERT INTO equipment_category(name) VALUES(#{name})") @Options(useGeneratedKeys=true,keyProperty="id") int insertCategory(CategoryRow category);
 @Update("UPDATE equipment_category SET name=#{name} WHERE id=#{id}") int updateCategory(CategoryRow category);
 @Delete("DELETE FROM equipment_category WHERE id=#{id}") int deleteCategory(long id);
 @Select("SELECT COUNT(*) FROM equipment WHERE category_id=#{id}") long categoryEquipment(long id);

 @Select(EQUIPMENT_VIEW + " WHERE e.id=#{id}") EquipmentRow equipment(long id);
 @Select("SELECT * FROM equipment WHERE id=#{id} FOR UPDATE") EquipmentRow lockEquipment(long id);
 @Select({"<script>", EQUIPMENT_VIEW, " WHERE 1=1",
  "<if test='onlyEnabled'> AND e.enabled=TRUE</if>",
  "<if test='categoryId != null'> AND e.category_id=#{categoryId}</if>",
  "<if test='keyword != null and keyword != &quot;&quot;'> AND (LOCATE(#{keyword},e.name)>0 OR LOCATE(#{keyword},e.description)>0)</if>",
  " ORDER BY e.id DESC</script>"}) List<EquipmentRow> equipmentList(@Param("keyword") String keyword,@Param("categoryId") Long categoryId,@Param("onlyEnabled") boolean onlyEnabled);
 @Insert("INSERT INTO equipment(name,description,category_id,total_quantity,available_quantity,enabled) VALUES(#{name},#{description},#{categoryId},#{totalQuantity},#{availableQuantity},#{enabled})")
 @Options(useGeneratedKeys=true,keyProperty="id") int insertEquipment(EquipmentRow equipment);
 @Update("UPDATE equipment SET name=#{name},description=#{description},category_id=#{categoryId},total_quantity=#{totalQuantity},available_quantity=#{availableQuantity},enabled=#{enabled} WHERE id=#{id}") int updateEquipment(EquipmentRow equipment);
 @Delete("DELETE FROM equipment WHERE id=#{id}") int deleteEquipment(long id);
 @Select("SELECT COUNT(*) FROM borrow_record WHERE equipment_id=#{id}") long equipmentHistory(long id);
 @Update("UPDATE equipment SET available_quantity=available_quantity-1 WHERE id=#{id} AND enabled=TRUE AND available_quantity>0") int takeStock(long id);
 @Update("UPDATE equipment SET available_quantity=available_quantity+1 WHERE id=#{id} AND available_quantity<total_quantity") int restoreStock(long id);

 @Insert("INSERT INTO borrow_record(user_id,equipment_id,borrow_time,status) VALUES(#{userId},#{equipmentId},#{borrowTime},'BORROWING')")
 @Options(useGeneratedKeys=true,keyProperty="id") int insertBorrow(BorrowRow borrow);
 @Select(BORROW_VIEW + " WHERE b.id=#{id}") BorrowRow borrow(long id);
 @Select("SELECT * FROM borrow_record WHERE id=#{id} FOR UPDATE") BorrowRow lockBorrow(long id);
 @Update("UPDATE borrow_record SET status='RETURNED',return_time=#{returnTime} WHERE id=#{id} AND user_id=#{userId} AND status='BORROWING'")
 int returnBorrow(@Param("id") long id,@Param("userId") long userId,@Param("returnTime") LocalDateTime returnTime);
 @Select({"<script>",BORROW_VIEW," WHERE 1=1",
  "<if test='userId != null'> AND b.user_id=#{userId}</if>",
  "<if test='status != null'> AND b.status=#{status}</if>",
  "<if test='keyword != null and keyword != &quot;&quot;'> AND (LOCATE(#{keyword},e.name)>0 OR LOCATE(#{keyword},u.username)>0)</if>",
  "<if test='start != null'> AND b.borrow_time &gt;= #{start}</if>",
  "<if test='end != null'> AND b.borrow_time &lt; #{end}</if>",
  " ORDER BY b.borrow_time DESC,b.id DESC</script>"})
 List<BorrowRow> borrows(@Param("userId") Long userId,@Param("status") String status,@Param("keyword") String keyword,@Param("start") LocalDateTime start,@Param("end") LocalDateTime end);
 @Select("SELECT COALESCE(SUM(total_quantity),0) FROM equipment") long totalQuantity();
 @Select("SELECT COUNT(*) FROM equipment") long equipmentTypes();
 @Select("SELECT COALESCE(SUM(available_quantity),0) FROM equipment") long availableQuantity();
 @Select("SELECT COUNT(*) FROM borrow_record WHERE status='BORROWING'") long borrowedQuantity();
 @Select("SELECT COUNT(*) FROM users") long userCount();
}
