package com.ticket.config;

import com.ticket.common.JwtUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class AdminInterceptor implements HandlerInterceptor {

    private final JwtUtil jwtUtil;

    public AdminInterceptor(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response,
                             Object handler) throws Exception {
        String auth = request.getHeader("Authorization");
        if (auth == null || !auth.startsWith("Bearer ")) {
            response.setStatus(401);
            response.getWriter().write("{\"code\":401,\"message\":\"未登录\"}");
            return false;
        }
        try {
            String token = auth.substring(7);
            int role = jwtUtil.getRole(token);
            if (role != 1) {
                response.setStatus(403);
                response.getWriter().write("{\"code\":403,\"message\":\"无管理员权限\"}");
                return false;
            }
        } catch (Exception e) {
            response.setStatus(401);
            response.getWriter().write("{\"code\":401,\"message\":\"token无效\"}");
            return false;
        }
        return true;
    }
}
