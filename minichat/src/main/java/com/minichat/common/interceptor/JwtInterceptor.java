package com.minichat.common.interceptor;

import com.minichat.user.entity.User;
import com.minichat.user.mapper.UserMapper;
import com.minichat.common.util.JwtUtil;
import com.minichat.common.util.UserContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.io.PrintWriter;

@Component
@RequiredArgsConstructor
public class JwtInterceptor implements HandlerInterceptor {

    private final JwtUtil jwtUtil;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String token = request.getHeader("Authorization");
        if (token == null || token.isEmpty()) {
            token = request.getParameter("token");
        }
        if (token == null || token.isEmpty()) {
            response.setContentType("application/json;charset=UTF-8");
            PrintWriter out = response.getWriter();
            out.write("{\"code\":0,\"msg\":\"请先登录\",\"data\":null}");
            out.flush();
            out.close();
            return false;
        }
        if (token.regionMatches(true, 0, "Bearer ", 0, 7)) {
            token = token.substring(7).trim();
        }
        //4.判断token是否有效
        try{
            Long userId = jwtUtil.getUserIdFromToken(token);
            //6.如果有效，将用户id存入上下文（供控制器使用）
            UserContext.setCurUserId(userId);
            return true;
        }catch (Exception e){
            //6.如果无效，返回401错误
            response.setContentType("application/json;charset=UTF-8");
            PrintWriter out = response.getWriter();
            out.write("{\"code\":0,\"msg\":\"请先登录\",\"data\":null}");
            out.flush();
            out.close();
            return false;
        }
    }

    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        //7.清除上下文中的用户名
        UserContext.remove();
    }

}