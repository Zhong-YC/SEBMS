package com.sebms;
import java.time.LocalDateTime;
public final class Models {
 private Models() {}
 public static class UserRow {
  public Long id;
  public String username, passwordHash, displayName, role;
  public boolean enabled;
  public UserView view() { return new UserView(id, username, displayName, role, enabled); }
 }
 public static class CategoryRow { public Long id; public String name; }
 public static class EquipmentRow {
  public Long id, categoryId;
  public String name, description, categoryName;
  public int totalQuantity, availableQuantity;
  public boolean enabled;
 }
 public static class BorrowRow {
  public Long id, userId, equipmentId;
  public String username, displayName, equipmentName, status;
  public LocalDateTime borrowTime, returnTime;
 }
 public record UserView(Long id, String username, String displayName, String role, boolean enabled) {}
 public record LoginInput(String username, String password) {}
 public record ProfileInput(String displayName, String password) {}
 public record CategoryInput(String name) {}
 public record EquipmentInput(String name, String description, Long categoryId, Integer totalQuantity, Boolean enabled) {}
 public record BorrowInput(Long equipmentId) {}
 public record UserCreateInput(String username, String password, String displayName, String role, Boolean enabled) {}
 public record UserUpdateInput(String displayName, String role, Boolean enabled, String password) {}
 public record Stats(long totalQuantity, long equipmentTypes, long availableQuantity, long borrowedQuantity, long userCount) {}
 public record ApiResult<T>(int code, String message, T data) {
  public static <T> ApiResult<T> ok(T data) { return new ApiResult<>(0, "成功", data); }
 }
}
