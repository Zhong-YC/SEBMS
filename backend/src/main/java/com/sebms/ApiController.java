package com.sebms;

import com.sebms.Models.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import java.util.List;
import java.util.Map;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class ApiController {
 private final BusinessService service;
 public ApiController(BusinessService service) { this.service=service; }
 private long id(HttpServletRequest request) { return WebSecurity.actor(request).id; }
 @GetMapping("/health") public ApiResult<?> health() { return ApiResult.ok(Map.of("status","UP")); }
 @PostMapping("/auth/login") public ApiResult<UserView> login(@RequestBody LoginInput input,HttpServletRequest request) {
  UserRow user=service.login(input);
  HttpSession previous=request.getSession(false); if(previous!=null) previous.invalidate();
  WebSecurity.authenticate(request,user); return ApiResult.ok(user.view());
 }
 @GetMapping("/auth/me") public ApiResult<UserView> me(HttpServletRequest request) { return ApiResult.ok(WebSecurity.actor(request).view()); }
 @PostMapping("/auth/logout") public ApiResult<Void> logout(HttpServletRequest request) {
  HttpSession session=request.getSession(false); if(session!=null) session.invalidate(); return ApiResult.ok(null);
 }
 @PutMapping("/auth/profile") public ApiResult<UserView> profile(@RequestBody ProfileInput input,HttpServletRequest request) {
  UserRow user=service.profile(id(request),input); WebSecurity.authenticate(request,user); return ApiResult.ok(user.view());
 }
 @GetMapping("/categories") public ApiResult<List<CategoryRow>> categories() { return ApiResult.ok(service.categories()); }
 @PostMapping("/categories") public ApiResult<CategoryRow> createCategory(@RequestBody CategoryInput input,HttpServletRequest request) { return ApiResult.ok(service.createCategory(id(request),input)); }
 @PutMapping("/categories/{id}") public ApiResult<CategoryRow> updateCategory(@PathVariable long id,@RequestBody CategoryInput input,HttpServletRequest request) { return ApiResult.ok(service.updateCategory(id(request),id,input)); }
 @DeleteMapping("/categories/{id}") public ApiResult<Void> deleteCategory(@PathVariable long id,HttpServletRequest request) { service.deleteCategory(id(request),id); return ApiResult.ok(null); }
 @GetMapping("/equipment") public ApiResult<List<EquipmentRow>> equipment(@RequestParam(required=false) String keyword,@RequestParam(required=false) Long categoryId,HttpServletRequest request) { return ApiResult.ok(service.equipment(WebSecurity.actor(request),keyword,categoryId)); }
 @PostMapping("/equipment") public ApiResult<EquipmentRow> createEquipment(@RequestBody EquipmentInput input,HttpServletRequest request) { return ApiResult.ok(service.createEquipment(id(request),input)); }
 @PutMapping("/equipment/{id}") public ApiResult<EquipmentRow> updateEquipment(@PathVariable long id,@RequestBody EquipmentInput input,HttpServletRequest request) { return ApiResult.ok(service.updateEquipment(id(request),id,input)); }
 @DeleteMapping("/equipment/{id}") public ApiResult<Void> deleteEquipment(@PathVariable long id,HttpServletRequest request) { service.deleteEquipment(id(request),id); return ApiResult.ok(null); }
 @PostMapping("/borrows") public ApiResult<BorrowRow> borrow(@RequestBody BorrowInput input,HttpServletRequest request) { return ApiResult.ok(service.borrow(id(request),input)); }
 @PostMapping("/borrows/{id}/return") public ApiResult<BorrowRow> returnBorrow(@PathVariable long id,HttpServletRequest request) { return ApiResult.ok(service.returnBorrow(id(request),id)); }
 @GetMapping("/borrows") public ApiResult<List<BorrowRow>> borrows(@RequestParam(required=false) String status,@RequestParam(required=false) String keyword,@RequestParam(required=false) String startDate,@RequestParam(required=false) String endDate,HttpServletRequest request) {
  return ApiResult.ok(service.borrows(WebSecurity.actor(request),status,keyword,startDate,endDate));
 }
 @GetMapping("/users") public ApiResult<List<UserView>> users(HttpServletRequest request) { return ApiResult.ok(service.users(WebSecurity.actor(request))); }
 @PostMapping("/users") public ApiResult<UserView> createUser(@RequestBody UserCreateInput input,HttpServletRequest request) { return ApiResult.ok(service.createUser(id(request),input)); }
 @PutMapping("/users/{id}") public ApiResult<UserView> updateUser(@PathVariable long id,@RequestBody UserUpdateInput input,HttpServletRequest request) { return ApiResult.ok(service.updateUser(id(request),id,input)); }
 @DeleteMapping("/users/{id}") public ApiResult<Void> deleteUser(@PathVariable long id,HttpServletRequest request) { service.deleteUser(id(request),id); return ApiResult.ok(null); }
 @GetMapping("/stats") public ApiResult<Stats> stats(HttpServletRequest request) { return ApiResult.ok(service.stats(WebSecurity.actor(request))); }
}
