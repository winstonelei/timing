package com.timing.job.admin.controller;

import com.timing.executor.core.biz.model.ReturnT;
import com.timing.job.admin.controller.annoation.PermessionLimit;
import com.timing.job.admin.controller.interceptor.PermissionInterceptor;
import com.timing.job.admin.core.util.PropertiesUtil;
import com.timing.job.admin.service.TimingJobService;
import org.apache.commons.lang.StringUtils;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Map;

/**
 * index controller
 */
@Controller
public class IndexController {

	@Resource
	private TimingJobService timingJobService;

	@RequestMapping("/")
	public String index(Model model) {

		Map<String, Object> dashboardMap = timingJobService.dashboardInfo();
		model.addAllAttributes(dashboardMap);

		return "index";
	}

    @RequestMapping("/triggerChartDate")
	@ResponseBody
	public ReturnT<Map<String, Object>> triggerChartDate() {
        ReturnT<Map<String, Object>> triggerChartDate = timingJobService.triggerChartDate();
        return triggerChartDate;
    }
	
	@RequestMapping("/toLogin")
	@PermessionLimit(limit=false)
	public String toLogin(Model model, HttpServletRequest request) {
		if (PermissionInterceptor.ifLogin(request)) {
			return "redirect:/";
		}
		return "login";
	}
	
	@RequestMapping(value="login", method=RequestMethod.POST)
	@ResponseBody
	@PermessionLimit(limit=false)
	public ReturnT<String> loginDo(HttpServletRequest request, HttpServletResponse response, String userName, String password, String ifRemember){
		if (!PermissionInterceptor.ifLogin(request)) {
			if (StringUtils.isNotBlank(userName) && StringUtils.isNotBlank(password)
					&& PropertiesUtil.getString("timing.job.login.username").equals(userName)
					&& PropertiesUtil.getString("timing.job.login.password").equals(password)) {
				boolean ifRem = false;
				if (StringUtils.isNotBlank(ifRemember) && "on".equals(ifRemember)) {
					ifRem = true;
				}
				PermissionInterceptor.login(response, ifRem);
			} else {
				return new ReturnT<String>(500, "账号或密码错误");
			}
		}
		return ReturnT.SUCCESS;
	}
	
	@RequestMapping(value="logout", method=RequestMethod.POST)
	@ResponseBody
	@PermessionLimit(limit=false)
	public ReturnT<String> logout(HttpServletRequest request, HttpServletResponse response){
		if (PermissionInterceptor.ifLogin(request)) {
			PermissionInterceptor.logout(request, response);
		}
		return ReturnT.SUCCESS;
	}
	
	@RequestMapping("/help")
	public String help() {

		/*if (!PermissionInterceptor.ifLogin(request)) {
			return "redirect:/toLogin";
		}*/

		return "help";
	}
	
}
