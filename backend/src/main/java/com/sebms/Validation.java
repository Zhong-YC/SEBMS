package com.sebms;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
final class Validation {
 private Validation() {}
 static String text(String value, String label, int min, int max) {
  if (value == null) throw ApiException.bad(label + "不能为空");
  String result = value.trim();
  if (result.length() < min || result.length() > max) throw ApiException.bad(label + "长度须为" + min + "至" + max + "个字符");
  return result;
 }
 static String username(String value) {
  String result = text(value, "用户名", 3, 30);
  if (!result.matches("[A-Za-z0-9_]+")) throw ApiException.bad("用户名只允许英文字母、数字和下划线");
  return result;
 }
 static String password(String value) {
  String result = text(value, "密码", 8, 64);
  if (result.getBytes(StandardCharsets.UTF_8).length > 72) throw ApiException.bad("密码的UTF-8长度不能超过72字节");
  return result;
 }
 static String optionalPassword(String value) { return value == null || value.isBlank() ? null : password(value); }
 static long id(Long value, String label) {
  if (value == null || value <= 0) throw ApiException.bad(label + "无效");
  return value;
 }
 static int quantity(Integer value) {
  if (value == null || value < 0 || value > 1_000_000) throw ApiException.bad("器材总数量须为0至1000000的整数");
  return value;
 }
 static boolean enabled(Boolean value) {
  if (value == null) throw ApiException.bad("启用状态不能为空"); return value;
 }
 static String role(String value) {
  String role = text(value, "角色", 4, 5);
  if (!role.equals("ADMIN") && !role.equals("USER")) throw ApiException.bad("角色须为ADMIN或USER");
  return role;
 }
 static String keyword(String value) { return value == null ? "" : text(value, "关键词", 0, 100); }
 static LocalDate date(String value, String label) {
  if (value == null || value.isBlank()) return null;
  String input = value.trim();
  if (!input.matches("\\d{4}-\\d{2}-\\d{2}")) throw ApiException.bad(label + "须为YYYY-MM-DD");
  try { return LocalDate.parse(input); } catch (DateTimeParseException ex) { throw ApiException.bad(label + "无效"); }
 }
}
