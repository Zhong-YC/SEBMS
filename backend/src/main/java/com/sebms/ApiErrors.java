package com.sebms;

import com.sebms.Models.ApiResult;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@RestControllerAdvice
public class ApiErrors {
 private static final Logger LOG=LoggerFactory.getLogger(ApiErrors.class);
 private ResponseEntity<ApiResult<Void>> error(int code,String message) { return ResponseEntity.status(code).body(new ApiResult<>(code,message,null)); }
 @ExceptionHandler(ApiException.class) public ResponseEntity<ApiResult<Void>> business(ApiException ex) { return error(ex.status,ex.getMessage()); }
 @ExceptionHandler({HttpMessageNotReadableException.class,MethodArgumentTypeMismatchException.class,MissingServletRequestParameterException.class})
 public ResponseEntity<ApiResult<Void>> invalid(Exception ex) { return error(400,"请求参数无效，请检查字段、类型和JSON格式"); }
 @ExceptionHandler(DataIntegrityViolationException.class) public ResponseEntity<ApiResult<Void>> integrity(Exception ex) { return error(409,"数据重复或存在关联记录，请检查用户名、分类名称或关联关系"); }
 @ExceptionHandler(DataAccessException.class) public ResponseEntity<ApiResult<Void>> database(Exception ex) { return error(409,"数据正在被其他请求修改，请稍后重试"); }
 @ExceptionHandler(HttpRequestMethodNotSupportedException.class) public ResponseEntity<ApiResult<Void>> method(Exception ex) { return error(405,"请求方法不支持"); }
 @ExceptionHandler(NoResourceFoundException.class) public ResponseEntity<ApiResult<Void>> missing(Exception ex) { return error(404,"接口不存在"); }
 @ExceptionHandler(Exception.class) public ResponseEntity<ApiResult<Void>> unexpected(Exception ex) {
  LOG.error("处理请求时发生异常：{}",ex.getClass().getSimpleName()); return error(500,"服务器处理失败，请联系管理员");
 }

 @RestController
 static class Fallback implements ErrorController {
  @RequestMapping("/error") public ResponseEntity<ApiResult<Void>> fallback(HttpServletRequest request) {
   Object value=request.getAttribute("jakarta.servlet.error.status_code");
   int status=value instanceof Integer code ? code : 500;
   return ResponseEntity.status(status).body(new ApiResult<>(status,status==404?"接口不存在":"请求处理失败",null));
  }
 }
}
