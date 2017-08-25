package com.timing.job.admin.controller.resolver;

import com.timing.executor.core.biz.model.ReturnT;
import com.timing.executor.core.biz.util.JacksonUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerExceptionResolver;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Created by winstone on 2017/8/23.
 */
public class WebExceptionResolver implements HandlerExceptionResolver {


    private static transient Logger logger = LoggerFactory.getLogger(WebExceptionResolver.class);


    @Override
    public ModelAndView resolveException(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse, Object handler, Exception e) {
        logger.error("WebExceptionResolver:{}", e);

        ModelAndView mv = new ModelAndView();
        HandlerMethod method = (HandlerMethod)handler;
        ResponseBody responseBody = method.getMethodAnnotation(ResponseBody.class);
        if (responseBody != null) {
            httpServletResponse.setContentType("application/json;charset=UTF-8");
            mv.addObject("result", JacksonUtil.writeValueAsString(new ReturnT<String>(500, e.toString().replaceAll("\n", "<br/>"))));
            mv.setViewName("/common/common.result");
        } else {
            mv.addObject("exceptionMsg", e.toString().replaceAll("\n", "<br/>"));
            mv.setViewName("/common/common.exception");
        }
        return mv;
    }
}
