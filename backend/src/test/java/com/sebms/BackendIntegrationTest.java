package com.sebms;

import static org.assertj.core.api.Assertions.assertThat;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sebms.Models.*;
import java.util.*;
import java.net.URI;
import java.util.concurrent.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.init.ScriptUtils;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.support.EncodedResource;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.*;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

@SpringBootTest
@AutoConfigureMockMvc(print=org.springframework.boot.test.autoconfigure.web.servlet.MockMvcPrint.NONE)
class BackendIntegrationTest {
 @Autowired MockMvc mvc;
 @Autowired ObjectMapper json;
 @Autowired JdbcTemplate jdbc;
 @Autowired Store store;
 @Autowired BusinessService service;
 private static final String ADMIN_HASH=new BCryptPasswordEncoder(12).encode("Admin123!");
 private static final String USER_HASH=new BCryptPasswordEncoder(12).encode("Student123!");
 private long adminId,userId,secondId,categoryId;
 private MockHttpSession admin,user,second;

 @DynamicPropertySource static void database(DynamicPropertyRegistry registry) {
  String url=System.getenv("TEST_DB_URL");
  if(url!=null && !url.isBlank()) {
   // 本类会清理夹具，只允许明确命名的独立测试数据库。
   if(!url.matches("jdbc:mysql://[^/]+/[A-Za-z0-9_]+_test(?:\\?.*)?")) throw new IllegalStateException("TEST_DB_URL必须指向独立的*_test数据库");
   registry.add("spring.datasource.url",()->url);
   registry.add("spring.datasource.username",()->System.getenv().getOrDefault("TEST_DB_USER","sebms"));
   registry.add("spring.datasource.password",()->System.getenv().getOrDefault("TEST_DB_PASSWORD",""));
  } else {
   registry.add("spring.datasource.url",()->"jdbc:h2:mem:sebms_test;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1;LOCK_TIMEOUT=10000");
   registry.add("spring.datasource.username",()->"sa"); registry.add("spring.datasource.password",()->"");
  }
  registry.add("sebms.demo-data",()->false);
 }
 @BeforeEach void fixtures() throws Exception {
  jdbc.update("DELETE FROM borrow_record"); jdbc.update("DELETE FROM equipment");
  jdbc.update("DELETE FROM equipment_category"); jdbc.update("DELETE FROM users");
  adminId=insertUser("admin","ADMIN",ADMIN_HASH); userId=insertUser("student","USER",USER_HASH); secondId=insertUser("second","USER",USER_HASH);
  CategoryRow category=new CategoryRow(); category.name="球类器材"; store.insertCategory(category); categoryId=category.id;
  admin=login("admin","Admin123!"); user=login("student","Student123!"); second=login("second","Student123!");
 }
 private long insertUser(String name,String role,String hash) {
  UserRow row=new UserRow(); row.username=name; row.displayName=name; row.role=role; row.passwordHash=hash; row.enabled=true;
  store.insertUser(row); return row.id;
 }
 private MockHttpSession login(String username,String password) throws Exception {
  MvcResult result=call("POST","/api/auth/login",null,Map.of("username",username,"password",password));
  success(result); return (MockHttpSession)result.getRequest().getSession(false);
 }
 private MvcResult call(String method,String path,MockHttpSession session,Object body) throws Exception {
  MockHttpServletRequestBuilder request=MockMvcRequestBuilders.request(HttpMethod.valueOf(method),URI.create(path));
  if(session!=null) request.session(session);
  if(body!=null) request.contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsBytes(body));
  return mvc.perform(request).andReturn();
 }
 private JsonNode body(MvcResult result) throws Exception { return json.readTree(result.getResponse().getContentAsByteArray()); }
 private JsonNode success(MvcResult result) throws Exception {
  assertThat(result.getResponse().getStatus()).as(result.getResponse().getContentAsString()).isEqualTo(200);
  assertThat(body(result).get("code").asInt()).isZero(); return body(result).get("data");
 }
 private void error(MvcResult result,int code) throws Exception {
  assertThat(result.getResponse().getStatus()).as(result.getResponse().getContentAsString()).isEqualTo(code);
  assertThat(body(result).get("code").asInt()).isEqualTo(code); assertThat(body(result).get("data").isNull()).isTrue();
 }
 private long equipment(int quantity) throws Exception {
  return success(call("POST","/api/equipment",admin,equipmentInput(quantity,true))).get("id").asLong();
 }
 private Map<String,Object> equipmentInput(Object quantity,boolean enabled) { return Map.of("name","篮球","description","测试器材","categoryId",categoryId,"totalQuantity",quantity,"enabled",enabled); }
 private long borrow(long equipmentId,MockHttpSession session) throws Exception { return success(call("POST","/api/borrows",session,Map.of("equipmentId",equipmentId))).get("id").asLong(); }
 private void invariant(long equipmentId) {
  EquipmentRow row=store.equipment(equipmentId);
  long active=jdbc.queryForObject("SELECT COUNT(*) FROM borrow_record WHERE equipment_id=? AND status='BORROWING'",Long.class,equipmentId);
  assertThat(row.totalQuantity).isEqualTo(row.availableQuantity+active);
 }

 @Test void authenticationAndUnifiedErrors() throws Exception {
  assertThat(success(call("GET","/api/health",null,null)).get("status").asText()).isEqualTo("UP");
  error(call("GET","/api/auth/me",null,null),401);
  error(call("POST","/api/auth/login",null,Map.of("username","admin","password","Wrong123!")),401);
  JsonNode current=success(call("GET","/api/auth/me",admin,null));
  assertThat(current.get("role").asText()).isEqualTo("ADMIN"); assertThat(current.has("passwordHash")).isFalse(); assertThat(current.has("password")).isFalse();
  success(call("POST","/api/auth/logout",user,null)); error(call("GET","/api/auth/me",user,null),401);
  error(call("GET","/api/not-existing",admin,null),404);
 }
 @Test void originRoleAndInputValidation() throws Exception {
  MvcResult foreign=mvc.perform(MockMvcRequestBuilders.post("/api/categories").session(admin).header("Origin","https://foreign.example").contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"不允许\"}")).andReturn();
  error(foreign,403);
  MvcResult local=mvc.perform(MockMvcRequestBuilders.post("/api/categories").session(admin).header("Origin","http://localhost:5173").contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"本地分类\"}")).andReturn(); success(local);
  error(call("POST","/api/categories",user,Map.of("name","无权限")),403);
  error(call("GET","/api/users",user,null),403); error(call("GET","/api/stats",user,null),403);
  error(call("POST","/api/equipment",admin,equipmentInput(-1,true)),400);
  error(call("POST","/api/equipment",admin,equipmentInput(1.5,true)),400);
  error(call("POST","/api/equipment",admin,Map.of("name","篮球")),400);
  error(call("PUT","/api/auth/profile",user,Map.of("displayName","学生","role","ADMIN")),400);
  error(call("POST","/api/categories",admin,Map.of("name"," ")),400);
  error(call("POST","/api/users",admin,Map.of("username","bad-name","password","Password1!","displayName","测试","role","USER","enabled",true)),400);
  error(call("GET","/api/borrows?startDate=2026-02-30",user,null),400);
  error(call("GET","/api/borrows?startDate=2026-10-12&endDate=2026-10-11",user,null),400);
 }
 @Test void profileAndChangedPasswordInvalidateOtherSessions() throws Exception {
  MockHttpSession old=login("student","Student123!");
  JsonNode profile=success(call("PUT","/api/auth/profile",user,Map.of("displayName","  新姓名  ","password","NewPass123!")));
  assertThat(profile.get("displayName").asText()).isEqualTo("新姓名");
  assertThat(profile.get("role").asText()).isEqualTo("USER");
  success(call("GET","/api/auth/me",user,null)); error(call("GET","/api/auth/me",old,null),401);
  error(call("POST","/api/auth/login",null,Map.of("username","student","password","Student123!")),401);
  login("student","NewPass123!");
  assertThat(store.user(userId).passwordHash).startsWith("$2a$");
 }
 @Test void borrowOwnershipStockAndRepeatedReturn() throws Exception {
  long item=equipment(1),record=borrow(item,user);
  assertThat(store.equipment(item).availableQuantity).isZero(); invariant(item);
  error(call("POST","/api/borrows",second,Map.of("equipmentId",item)),409);
  error(call("POST","/api/borrows",admin,Map.of("equipmentId",item)),403);
  error(call("POST","/api/borrows/"+record+"/return",second,null),403);
  error(call("POST","/api/borrows/"+record+"/return",admin,null),403);
  assertThat(success(call("GET","/api/borrows?userId="+userId,second,null)).size()).isZero();
  assertThat(success(call("GET","/api/borrows?status=BORROWING&keyword=student",admin,null)).size()).isEqualTo(1);
  error(call("PUT","/api/equipment/"+item,admin,equipmentInput(0,true)),409);
  JsonNode returned=success(call("POST","/api/borrows/"+record+"/return",user,null));
  assertThat(returned.get("status").asText()).isEqualTo("RETURNED"); assertThat(returned.get("returnTime").isTextual()).isTrue();
  error(call("POST","/api/borrows/"+record+"/return",user,null),409);
  assertThat(store.equipment(item).availableQuantity).isEqualTo(1); invariant(item);
 }
 @Test void linkedDeletesAndLastAdministratorAreProtected() throws Exception {
  long item=equipment(1),record=borrow(item,user);
  success(call("POST","/api/borrows/"+record+"/return",user,null));
  error(call("DELETE","/api/categories/"+categoryId,admin,null),409);
  error(call("DELETE","/api/equipment/"+item,admin,null),409);
  error(call("DELETE","/api/users/"+userId,admin,null),409);
  error(call("DELETE","/api/users/"+adminId,admin,null),409);
  error(call("PUT","/api/users/"+adminId,admin,Map.of("displayName","管理员","role","USER","enabled",true)),409);
  error(call("PUT","/api/users/"+adminId,admin,Map.of("displayName","管理员","role","ADMIN","enabled",false)),409);
  long empty=equipment(0); success(call("DELETE","/api/equipment/"+empty,admin,null));
  success(call("DELETE","/api/users/"+secondId,admin,null));
 }
 @Test void disabledUsersAndEquipmentAreEnforced() throws Exception {
  long item=equipment(1);
  success(call("PUT","/api/equipment/"+item,admin,equipmentInput(1,false)));
  error(call("POST","/api/borrows",user,Map.of("equipmentId",item)),409);
  assertThat(success(call("GET","/api/equipment",user,null)).size()).isZero();
  assertThat(success(call("GET","/api/equipment",admin,null)).size()).isEqualTo(1);
  success(call("PUT","/api/users/"+userId,admin,Map.of("displayName","停用学生","role","USER","enabled",false)));
  error(call("GET","/api/auth/me",user,null),401);
  error(call("POST","/api/auth/login",null,Map.of("username","student","password","Student123!")),403);
 }
 @Test void concurrentLastItemOnlyOneSucceeds() throws Exception {
  long item=equipment(1);
  ExecutorService pool=Executors.newFixedThreadPool(2); CyclicBarrier start=new CyclicBarrier(2);
  try {
   Future<MvcResult> a=pool.submit(()->{start.await(10,TimeUnit.SECONDS);return call("POST","/api/borrows",user,Map.of("equipmentId",item));});
   Future<MvcResult> b=pool.submit(()->{start.await(10,TimeUnit.SECONDS);return call("POST","/api/borrows",second,Map.of("equipmentId",item));});
   List<Integer> statuses=List.of(a.get(20,TimeUnit.SECONDS).getResponse().getStatus(),b.get(20,TimeUnit.SECONDS).getResponse().getStatus());
   assertThat(statuses).containsExactlyInAnyOrder(200,409);
   assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM borrow_record WHERE equipment_id=?",Long.class,item)).isEqualTo(1);
   invariant(item);
  } finally { pool.shutdownNow(); }
 }
 @Test void concurrentRepeatedReturnRestoresExactlyOneItem() throws Exception {
  long item=equipment(1),record=borrow(item,user);
  MockHttpSession another=login("student","Student123!");
  ExecutorService pool=Executors.newFixedThreadPool(2); CyclicBarrier start=new CyclicBarrier(2);
  try {
   Future<MvcResult> a=pool.submit(()->{start.await(10,TimeUnit.SECONDS);return call("POST","/api/borrows/"+record+"/return",user,null);});
   Future<MvcResult> b=pool.submit(()->{start.await(10,TimeUnit.SECONDS);return call("POST","/api/borrows/"+record+"/return",another,null);});
   assertThat(List.of(a.get(20,TimeUnit.SECONDS).getResponse().getStatus(),b.get(20,TimeUnit.SECONDS).getResponse().getStatus())).containsExactlyInAnyOrder(200,409);
   assertThat(store.equipment(item).availableQuantity).isEqualTo(1); invariant(item);
  } finally { pool.shutdownNow(); }
 }
 @Test void quantityAdjustmentSearchAndStatistics() throws Exception {
  long item=equipment(3); borrow(item,user);
  EquipmentRow changed=service.updateEquipment(adminId,item,new EquipmentInput("  篮球  ","  训练器材  ",categoryId,5,true));
  assertThat(changed.availableQuantity).isEqualTo(4); invariant(item);
  assertThat(success(call("GET","/api/equipment?keyword=%E8%AE%AD%E7%BB%83&categoryId="+categoryId,user,null)).size()).isEqualTo(1);
  JsonNode stats=success(call("GET","/api/stats",admin,null));
  assertThat(stats.get("totalQuantity").asInt()).isEqualTo(5); assertThat(stats.get("availableQuantity").asInt()).isEqualTo(4);
  assertThat(stats.get("borrowedQuantity").asInt()).isEqualTo(1); assertThat(stats.get("equipmentTypes").asInt()).isEqualTo(1); assertThat(stats.get("userCount").asInt()).isEqualTo(3);
 }
 @Test void demoInitializationCanRepeatWithoutResettingData() throws Exception {
  long item=equipment(3); borrow(item,user);
  DemoData demo=new DemoData(store,jdbc,true);
  demo.run(null); long before=store.userCount(),types=store.equipmentTypes(); demo.run(null);
  assertThat(store.userCount()).isEqualTo(before); assertThat(store.equipmentTypes()).isEqualTo(types);
  assertThat(store.user(adminId).passwordHash).isEqualTo(ADMIN_HASH); invariant(item);
 }
 @Test void failedBorrowInsertRollsBackAlreadyDecrementedStock() throws Exception {
  long item=equipment(1);
  // 用真实数据库约束让库存扣减后的记录插入失败，验证整个事务回滚。
  jdbc.execute("ALTER TABLE borrow_record ADD CONSTRAINT chk_test_reject_borrow CHECK (equipment_id <> "+item+")");
  try {
   error(call("POST","/api/borrows",user,Map.of("equipmentId",item)),409);
   assertThat(store.equipment(item).availableQuantity).isEqualTo(1);
   assertThat(store.equipmentHistory(item)).isZero(); invariant(item);
  } finally {
   boolean mysql;
   try(var connection=jdbc.getDataSource().getConnection()) { mysql=connection.getMetaData().getDatabaseProductName().equals("MySQL"); }
   jdbc.execute("ALTER TABLE borrow_record DROP "+(mysql?"CHECK ":"CONSTRAINT ")+"chk_test_reject_borrow");
  }
  borrow(item,user); invariant(item);
 }
 @Test void failedStockRestoreRollsBackBorrowReturn() throws Exception {
  long item=equipment(1),record=borrow(item,user);
  // 模拟库存异常，归还记录更新成功后补库被条件更新拒绝。
  jdbc.update("UPDATE equipment SET available_quantity=total_quantity WHERE id=?",item);
  error(call("POST","/api/borrows/"+record+"/return",user,null),409);
  BorrowRow unchanged=store.borrow(record);
  assertThat(unchanged.status).isEqualTo("BORROWING"); assertThat(unchanged.returnTime).isNull();
  assertThat(store.equipment(item).availableQuantity).isEqualTo(1);
  jdbc.update("UPDATE equipment SET available_quantity=0 WHERE id=?",item);
  success(call("POST","/api/borrows/"+record+"/return",user,null)); invariant(item);
 }
 @ParameterizedTest
 @CsvSource({"USER,true","ADMIN,false"})
 void concurrentDemotionsOrDisablingPreserveOneEnabledAdministrator(String nextRole,boolean nextEnabled) throws Exception {
  long otherAdminId=insertUser("other_admin","ADMIN",ADMIN_HASH);
  MockHttpSession otherAdmin=login("other_admin","Admin123!");
  ExecutorService pool=Executors.newFixedThreadPool(2); CyclicBarrier start=new CyclicBarrier(2);
  try {
   Future<MvcResult> a=pool.submit(()->{start.await(10,TimeUnit.SECONDS);return call("PUT","/api/users/"+adminId,admin,Map.of("displayName","管理员一","role",nextRole,"enabled",nextEnabled));});
   Future<MvcResult> b=pool.submit(()->{start.await(10,TimeUnit.SECONDS);return call("PUT","/api/users/"+otherAdminId,otherAdmin,Map.of("displayName","管理员二","role",nextRole,"enabled",nextEnabled));});
   assertThat(List.of(a.get(20,TimeUnit.SECONDS).getResponse().getStatus(),b.get(20,TimeUnit.SECONDS).getResponse().getStatus())).containsExactlyInAnyOrder(200,409);
   assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM users WHERE role='ADMIN' AND enabled=TRUE",Long.class)).isEqualTo(1);
  } finally { pool.shutdownNow(); }
 }
 @Test void dateFiltersIncludeBothCalendarBoundariesAndStillEnforceOwnership() throws Exception {
  long item=equipment(4),first=borrow(item,user),last=borrow(item,user),outside=borrow(item,user),others=borrow(item,second);
  jdbc.update("UPDATE borrow_record SET borrow_time='2026-10-12 00:00:00' WHERE id=?",first);
  jdbc.update("UPDATE borrow_record SET borrow_time='2026-10-12 23:59:59' WHERE id=?",last);
  jdbc.update("UPDATE borrow_record SET borrow_time='2026-10-13 00:00:00' WHERE id=?",outside);
  jdbc.update("UPDATE borrow_record SET borrow_time='2026-10-12 12:00:00' WHERE id=?",others);
  assertThat(success(call("GET","/api/borrows?startDate=2026-10-12&endDate=2026-10-12",user,null)).size()).isEqualTo(2);
  assertThat(success(call("GET","/api/borrows?startDate=2026-10-12&endDate=2026-10-12",admin,null)).size()).isEqualTo(3);
  assertThat(success(call("GET","/api/borrows?keyword=%E7%AF%AE%E7%90%83",user,null)).size()).isEqualTo(3);
  invariant(item);
 }
 @Test void sqlDemoSeedIsRepeatableAndPreservesExistingInventoryAndPasswords() throws Exception {
  jdbc.update("DELETE FROM borrow_record"); jdbc.update("DELETE FROM equipment");
  jdbc.update("DELETE FROM equipment_category"); jdbc.update("DELETE FROM users");
  seedSql();
  BCryptPasswordEncoder passwords=new BCryptPasswordEncoder();
  assertThat(passwords.matches("Admin123!",store.userByUsername("admin").passwordHash)).isTrue();
  assertThat(passwords.matches("Student123!",store.userByUsername("student").passwordHash)).isTrue();
  admin=login("admin","Admin123!"); user=login("student","Student123!");
  JsonNode items=success(call("GET","/api/equipment",user,null));
  assertThat(items.size()).isEqualTo(3);
  assertThat(store.categoryByName("球类器材")).isNotNull();
  assertThat(items.findValuesAsText("name")).containsExactlyInAnyOrder("篮球","羽毛球拍","跳绳");
  long item=items.get(0).get("id").asLong(); borrow(item,user);
  int remaining=store.equipment(item).availableQuantity;
  success(call("PUT","/api/auth/profile",admin,Map.of("displayName","保留的管理员姓名","password","KeptAdmin123!")));
  String originalPassword=store.userByUsername("admin").passwordHash;
  seedSql();
  assertThat(store.userCount()).isEqualTo(2); assertThat(store.equipmentTypes()).isEqualTo(3);
  assertThat(store.userByUsername("admin").displayName).isEqualTo("保留的管理员姓名");
  assertThat(store.userByUsername("admin").passwordHash).isEqualTo(originalPassword);
  error(call("POST","/api/auth/login",null,Map.of("username","admin","password","Admin123!")),401);
  login("admin","KeptAdmin123!");
  assertThat(store.equipment(item).availableQuantity).isEqualTo(remaining); invariant(item);
 }
 private void seedSql() throws Exception {
  try(var connection=jdbc.getDataSource().getConnection()) {
   ScriptUtils.executeSqlScript(connection,new EncodedResource(new FileSystemResource("sql/demo-data.sql"),"UTF-8"));
  }
 }
 @Test void everyManagementMutationRequiresAdministrator() throws Exception {
  long item=equipment(1);
  error(call("POST","/api/users",user,Map.of("username","new_user","password","Password123!","displayName","新用户","role","USER","enabled",true)),403);
  error(call("PUT","/api/users/"+secondId,user,Map.of("displayName","篡改姓名","role","ADMIN","enabled",true)),403);
  error(call("DELETE","/api/users/"+secondId,user,null),403);
  error(call("PUT","/api/categories/"+categoryId,user,Map.of("name","篡改分类")),403);
  error(call("DELETE","/api/categories/"+categoryId,user,null),403);
  error(call("POST","/api/equipment",user,equipmentInput(1,true)),403);
  error(call("PUT","/api/equipment/"+item,user,equipmentInput(8,false)),403);
  error(call("DELETE","/api/equipment/"+item,user,null),403);
  assertThat(store.userCount()).isEqualTo(3); assertThat(store.user(secondId).role).isEqualTo("USER");
  assertThat(store.equipment(item).availableQuantity).isEqualTo(1); assertThat(store.equipment(item).enabled).isTrue();
  invariant(item);
 }
}
