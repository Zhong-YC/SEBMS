package com.sebms;

import com.sebms.Models.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.io.Serializable;
import java.util.Set;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.config.annotation.*;

@Configuration
public class WebSecurity implements WebMvcConfigurer {
 static final String PRINCIPAL="sebmsPrincipal", ACTOR="sebmsActor";
 record Principal(long id,String passwordHash) implements Serializable {}
 private final BusinessService service;
 public WebSecurity(BusinessService service) { this.service=service; }
 static UserRow actor(HttpServletRequest request) { return (UserRow)request.getAttribute(ACTOR); }
 static void authenticate(HttpServletRequest request,UserRow user) {
  request.getSession(true).setAttribute(PRINCIPAL,new Principal(user.id,user.passwordHash));
 }
 @Override public void addInterceptors(InterceptorRegistry registry) {
  registry.addInterceptor(new HandlerInterceptor() {
   @Override public boolean preHandle(HttpServletRequest request,HttpServletResponse response,Object handler) {
    String path=request.getRequestURI();
    if(path.equals("/api/health") || path.equals("/api/auth/login") || path.equals("/api/auth/logout")) return true;
    HttpSession session=request.getSession(false);
    Object principal=session==null?null:session.getAttribute(PRINCIPAL);
    if(!(principal instanceof Principal identity)) throw new ApiException(401,"请先登录");
    UserRow user=service.user(identity.id());
    if(user==null || !user.enabled || !user.passwordHash.equals(identity.passwordHash())) {
     session.invalidate(); throw new ApiException(401,"登录已失效，请重新登录");
    }
    request.setAttribute(ACTOR,user); return true;
   }
  }).addPathPatterns("/api/**");
 }

 @Component
 public static class OriginFilter extends OncePerRequestFilter {
  private static final Set<String> ALLOWED=Set.of("http://127.0.0.1:5173","http://localhost:5173","http://127.0.0.1:8080","http://localhost:8080");
  private final ObjectMapper json;
  public OriginFilter(ObjectMapper json) { this.json=json; }
  @Override protected void doFilterInternal(HttpServletRequest request,HttpServletResponse response,FilterChain chain) throws ServletException,IOException {
   String method=request.getMethod(),origin=request.getHeader("Origin");
   if(request.getRequestURI().startsWith("/api/") && !Set.of("GET","HEAD","OPTIONS").contains(method)
     && origin!=null && !ALLOWED.contains(origin)) {
    response.setStatus(403); response.setContentType("application/json;charset=UTF-8");
    json.writeValue(response.getWriter(),new ApiResult<>(403,"请求来源不允许",null)); return;
   }
   chain.doFilter(request,response);
  }
 }
}
