package com.timing.job.admin.controller.interceptor;

import com.timing.job.admin.controller.annoation.PermessionLimit;
import com.timing.job.admin.core.util.CookieUtil;
import com.timing.job.admin.core.util.PropertiesUtil;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.math.BigInteger;

/**
 * Created by winstone on 2017/8/23.
 */
public class PermissionInterceptor extends HandlerInterceptorAdapter {
    public static final String LOGIN_IDENTITY_KEY = "LOGIN_IDENTITY";
    public static final String LOGIN_IDENTITY_TOKEN;
    static {
        String username = PropertiesUtil.getString("timing.job.login.username");
        String password = PropertiesUtil.getString("timing.job.login.password");
        String temp = username + "_" + password;
        LOGIN_IDENTITY_TOKEN = new BigInteger(1, temp.getBytes()).toString(16);
    }

    public static boolean login(HttpServletResponse response, boolean ifRemember){
        CookieUtil.set(response, LOGIN_IDENTITY_KEY, LOGIN_IDENTITY_TOKEN, ifRemember);
        return true;
    }
    public static void logout(HttpServletRequest request, HttpServletResponse response){
        CookieUtil.remove(request, response, LOGIN_IDENTITY_KEY);
    }
    public static boolean ifLogin(HttpServletRequest request){
        String indentityInfo = CookieUtil.getValue(request, LOGIN_IDENTITY_KEY);
        if (indentityInfo==null || !LOGIN_IDENTITY_TOKEN.equals(indentityInfo.trim())) {
            return false;
        }
        return true;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {

        if (!(handler instanceof HandlerMethod)) {
            return super.preHandle(request, response, handler);
        }

        if (!ifLogin(request)) {
            HandlerMethod method = (HandlerMethod)handler;
            PermessionLimit permission = method.getMethodAnnotation(PermessionLimit.class);
            if (permission == null || permission.limit()) {
                response.sendRedirect(request.getContextPath() + "/toLogin");
                //request.getRequestDispatcher("/toLogin").forward(request, response);
                return false;
            }
        }

        return super.preHandle(request, response, handler);
    }

}
