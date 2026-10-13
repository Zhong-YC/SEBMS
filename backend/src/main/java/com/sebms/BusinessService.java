package com.sebms;

import com.sebms.Models.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BusinessService {
 private final Store store;
 private final BCryptPasswordEncoder passwords = new BCryptPasswordEncoder(12);
 private final String dummyHash = passwords.encode("UnknownUserPassword!");
 public BusinessService(Store store) { this.store = store; }

 public UserRow login(LoginInput input) {
  String username = Validation.username(input.username());
  String password = Validation.password(input.password());
  UserRow user = store.userByUsername(username);
  boolean matches = passwords.matches(password, user == null ? dummyHash : user.passwordHash);
  if (user == null || !matches) throw new ApiException(401,"用户名或密码错误");
  if (!user.enabled) throw new ApiException(403,"账号已停用，请联系管理员");
  return user;
 }
 public UserRow user(long id) { return store.user(id); }
 private UserRow active(long id) {
  UserRow user = store.lockUser(id);
  if (user == null || !user.enabled) throw new ApiException(401,"账号已停用或不存在，请重新登录");
  return user;
 }
 private void administrator(UserRow actor) {
  if (!"ADMIN".equals(actor.role)) throw new ApiException(403,"仅管理员可执行此操作");
 }
 @Transactional
 public UserRow profile(long actorId, ProfileInput input) {
  UserRow user = active(actorId);
  user.displayName = Validation.text(input.displayName(),"姓名",1,50);
  String password = Validation.optionalPassword(input.password());
  if (password != null) user.passwordHash = passwords.encode(password);
  store.updateUser(user);
  return user;
 }

 public List<CategoryRow> categories() { return store.categories(); }
 @Transactional
 public CategoryRow createCategory(long actorId, CategoryInput input) {
  administrator(active(actorId));
  CategoryRow category = new CategoryRow();
  category.name = Validation.text(input.name(),"分类名称",1,50);
  store.insertCategory(category);
  return category;
 }
 @Transactional
 public CategoryRow updateCategory(long actorId,long id,CategoryInput input) {
  administrator(active(actorId));
  CategoryRow category = store.lockCategory(Validation.id(id,"分类编号"));
  if (category == null) throw ApiException.missing("分类不存在");
  category.name = Validation.text(input.name(),"分类名称",1,50);
  store.updateCategory(category); return category;
 }
 @Transactional
 public void deleteCategory(long actorId,long id) {
  administrator(active(actorId));
  if (store.lockCategory(Validation.id(id,"分类编号")) == null) throw ApiException.missing("分类不存在");
  if (store.categoryEquipment(id)>0) throw ApiException.conflict("分类已关联器材，不能删除");
  store.deleteCategory(id);
 }

 public List<EquipmentRow> equipment(UserRow actor,String keyword,Long categoryId) {
  if (categoryId != null) Validation.id(categoryId,"分类编号");
  return store.equipmentList(Validation.keyword(keyword),categoryId,!"ADMIN".equals(actor.role));
 }
 private EquipmentRow equipmentInput(EquipmentInput input) {
  EquipmentRow row = new EquipmentRow();
  row.name = Validation.text(input.name(),"器材名称",1,100);
  row.description = input.description()==null ? "" : Validation.text(input.description(),"器材描述",0,500);
  row.categoryId = Validation.id(input.categoryId(),"分类编号");
  row.totalQuantity = Validation.quantity(input.totalQuantity());
  row.enabled = Validation.enabled(input.enabled()); return row;
 }
 @Transactional
 public EquipmentRow createEquipment(long actorId,EquipmentInput input) {
  administrator(active(actorId));
  EquipmentRow row = equipmentInput(input);
  if (store.lockCategory(row.categoryId)==null) throw ApiException.bad("分类不存在");
  row.availableQuantity = row.totalQuantity;
  store.insertEquipment(row); return store.equipment(row.id);
 }
 @Transactional
 public EquipmentRow updateEquipment(long actorId,long id,EquipmentInput input) {
  administrator(active(actorId));
  EquipmentRow row = equipmentInput(input);
  if (store.lockCategory(row.categoryId)==null) throw ApiException.bad("分类不存在");
  EquipmentRow old = store.lockEquipment(Validation.id(id,"器材编号"));
  if (old==null) throw ApiException.missing("器材不存在");
  int borrowed = old.totalQuantity-old.availableQuantity;
  if (row.totalQuantity<borrowed) throw ApiException.conflict("器材总数量不能小于已借出数量（"+borrowed+"）");
  row.id = id; row.availableQuantity = row.totalQuantity-borrowed;
  store.updateEquipment(row); return store.equipment(id);
 }
 @Transactional
 public void deleteEquipment(long actorId,long id) {
  administrator(active(actorId));
  if (store.lockEquipment(Validation.id(id,"器材编号"))==null) throw ApiException.missing("器材不存在");
  if (store.equipmentHistory(id)>0) throw ApiException.conflict("器材已有借用历史，不能删除，请停用器材");
  store.deleteEquipment(id);
 }

 @Transactional
 public BorrowRow borrow(long actorId,BorrowInput input) {
  UserRow actor = active(actorId);
  if (!"USER".equals(actor.role)) throw new ApiException(403,"仅普通用户可登记借用");
  long equipmentId = Validation.id(input.equipmentId(),"器材编号");
  // 条件更新获得器材行锁，只有最后一件库存的一个请求能够成功。
  if (store.takeStock(equipmentId)!=1) {
   EquipmentRow row = store.equipment(equipmentId);
   if (row==null) throw ApiException.missing("器材不存在");
   if (!row.enabled) throw ApiException.conflict("器材已停用，不能借用");
   throw ApiException.conflict("可借数量不足");
  }
  BorrowRow row = new BorrowRow(); row.userId=actor.id; row.equipmentId=equipmentId;
  row.borrowTime=LocalDateTime.now().withNano(0);
  store.insertBorrow(row); return store.borrow(row.id);
 }
 @Transactional
 public BorrowRow returnBorrow(long actorId,long id) {
  UserRow actor = active(actorId);
  BorrowRow row = store.lockBorrow(Validation.id(id,"借用记录编号"));
  if (row==null) throw ApiException.missing("借用记录不存在");
  if (!row.userId.equals(actor.id)) throw new ApiException(403,"只能归还自己的借用记录");
  if (!"BORROWING".equals(row.status)) throw ApiException.conflict("该记录已经归还，请勿重复操作");
  if (store.returnBorrow(id,actor.id,LocalDateTime.now().withNano(0))!=1) throw ApiException.conflict("该记录已经归还，请勿重复操作");
  if (store.restoreStock(row.equipmentId)!=1) throw ApiException.conflict("库存状态异常，归还未完成，请联系管理员");
  return store.borrow(id);
 }
 public List<BorrowRow> borrows(UserRow actor,String status,String keyword,String startDate,String endDate) {
  String selected = status==null || status.isBlank() ? null : status.trim();
  if (selected!=null && !selected.equals("BORROWING") && !selected.equals("RETURNED")) throw ApiException.bad("记录状态无效");
  LocalDate start = Validation.date(startDate,"开始日期"), end = Validation.date(endDate,"结束日期");
  if (start!=null && end!=null && start.isAfter(end)) throw ApiException.bad("开始日期不能晚于结束日期");
  return store.borrows("ADMIN".equals(actor.role) ? null : actor.id,selected,Validation.keyword(keyword),
   start==null?null:start.atStartOfDay(),end==null?null:end.plusDays(1).atStartOfDay());
 }

 public List<UserView> users(UserRow actor) {
  administrator(actor); return store.users().stream().map(UserRow::view).toList();
 }
 @Transactional
 public UserView createUser(long actorId,UserCreateInput input) {
  administrator(active(actorId));
  UserRow row = new UserRow(); row.username=Validation.username(input.username());
  row.passwordHash=passwords.encode(Validation.password(input.password()));
  row.displayName=Validation.text(input.displayName(),"姓名",1,50);
  row.role=Validation.role(input.role()); row.enabled=Validation.enabled(input.enabled());
  store.insertUser(row); return row.view();
 }
 @Transactional
 public UserView updateUser(long actorId,long id,UserUpdateInput input) {
  // 统一锁定管理员集合，避免两个请求同时移除最后两个有效管理员。
  List<UserRow> admins=store.lockAdmins();
  administrator(active(actorId));
  UserRow row=store.lockUser(Validation.id(id,"用户编号"));
  if(row==null) throw ApiException.missing("用户不存在");
  String role=Validation.role(input.role()); boolean enabled=Validation.enabled(input.enabled());
  protectLastAdmin(row,role,enabled,admins);
  row.displayName=Validation.text(input.displayName(),"姓名",1,50); row.role=role; row.enabled=enabled;
  String password=Validation.optionalPassword(input.password());
  if(password!=null) row.passwordHash=passwords.encode(password);
  store.updateUser(row); return row.view();
 }
 private void protectLastAdmin(UserRow row,String nextRole,boolean nextEnabled,List<UserRow> admins) {
  if(row.enabled && "ADMIN".equals(row.role) && (!nextEnabled || !"ADMIN".equals(nextRole))
   && admins.stream().filter(a->a.enabled).count()<=1) throw ApiException.conflict("至少保留一个启用的管理员账号");
 }
 @Transactional
 public void deleteUser(long actorId,long id) {
  List<UserRow> admins=store.lockAdmins(); administrator(active(actorId));
  if(actorId==id) throw ApiException.conflict("不能删除当前登录账号");
  UserRow row=store.lockUser(Validation.id(id,"用户编号"));
  if(row==null) throw ApiException.missing("用户不存在");
  protectLastAdmin(row,"USER",false,admins);
  if(store.userHistory(id)>0) throw ApiException.conflict("用户已有借用历史，不能删除，请停用账号");
  store.deleteUser(id);
 }
 @Transactional(readOnly=true)
 public Stats stats(UserRow actor) {
  administrator(actor);
  return new Stats(store.totalQuantity(),store.equipmentTypes(),store.availableQuantity(),store.borrowedQuantity(),store.userCount());
 }
}
