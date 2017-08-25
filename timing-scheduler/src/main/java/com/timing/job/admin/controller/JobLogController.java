package com.timing.job.admin.controller;

import com.timing.executor.core.biz.ExecutorService;
import com.timing.executor.core.biz.model.LogResult;
import com.timing.executor.core.biz.model.ReturnT;
import com.timing.job.admin.core.model.TimingJobGroup;
import com.timing.job.admin.core.model.TimingJobInfo;
import com.timing.job.admin.core.model.TimingJobLog;
import com.timing.job.admin.core.schedule.TimingJobScheduler;
import com.timing.job.admin.dao.TimingJobGroupDao;
import com.timing.job.admin.dao.TimingJobInfoDao;
import com.timing.job.admin.dao.TimingJobLogDao;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.time.DateUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.annotation.Resource;
import java.text.ParseException;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;



@Controller
@RequestMapping("/joblog")
public class JobLogController {
	private static Logger logger = LoggerFactory.getLogger(JobLogController.class);

	@Resource
	private TimingJobGroupDao timingJobGroupDao;
	@Resource
	public TimingJobInfoDao timingJobInfoDao;
	@Resource
	public TimingJobLogDao timingJobLogDao;

	@RequestMapping
	public String index(Model model, @RequestParam(required = false, defaultValue = "0") Integer jobId) {

		// 执行器列表
		List<TimingJobGroup> jobGroupList =  timingJobGroupDao.findAll();
		model.addAttribute("JobGroupList", jobGroupList);

		// 任务
		if (jobId > 0) {
			TimingJobInfo jobInfo = timingJobInfoDao.loadById(jobId);
			model.addAttribute("jobInfo", jobInfo);
		}

		return "joblog/joblog.index";
	}

	@RequestMapping("/getJobsByGroup")
	@ResponseBody
	public ReturnT<List<TimingJobInfo>> getJobsByGroup(int jobGroup){
		List<TimingJobInfo> list = timingJobInfoDao.getJobsByGroup(jobGroup);
		return new ReturnT<List<TimingJobInfo>>(list);
	}
	
	@RequestMapping("/pageList")
	@ResponseBody
	public Map<String, Object> pageList(@RequestParam(required = false, defaultValue = "0") int start,  
			@RequestParam(required = false, defaultValue = "10") int length,
			int jobGroup, int jobId, int logStatus, String filterTime) {
		
		// parse param
		Date triggerTimeStart = null;
		Date triggerTimeEnd = null;
		if (StringUtils.isNotBlank(filterTime)) {
			String[] temp = filterTime.split(" - ");
			if (temp!=null && temp.length == 2) {
				try {
					triggerTimeStart = DateUtils.parseDate(temp[0], new String[]{"yyyy-MM-dd HH:mm:ss"});
					triggerTimeEnd = DateUtils.parseDate(temp[1], new String[]{"yyyy-MM-dd HH:mm:ss"});
				} catch (ParseException e) {	}
			}
		}
		
		// page query
		List<TimingJobLog> list = timingJobLogDao.pageList(start, length, jobGroup, jobId, triggerTimeStart, triggerTimeEnd, logStatus);
		int list_count = timingJobLogDao.pageListCount(start, length, jobGroup, jobId, triggerTimeStart, triggerTimeEnd, logStatus);
		
		// package result
		Map<String, Object> maps = new HashMap<String, Object>();
	    maps.put("recordsTotal", list_count);		// 总记录数
	    maps.put("recordsFiltered", list_count);	// 过滤后的总记录数
	    maps.put("data", list);  					// 分页列表
		return maps;
	}

	@RequestMapping("/logDetailPage")
	public String logDetailPage(int id, Model model){

		// base check
		ReturnT<String> logStatue = ReturnT.SUCCESS;
		TimingJobLog jobLog = timingJobLogDao.load(id);
		if (jobLog == null) {
            throw new RuntimeException("抱歉，日志ID非法.");
		}

        model.addAttribute("triggerCode", jobLog.getTriggerCode());
        model.addAttribute("handleCode", jobLog.getHandleCode());
        model.addAttribute("executorAddress", jobLog.getExecutorAddress());
        model.addAttribute("triggerTime", jobLog.getTriggerTime().getTime());
        model.addAttribute("logId", jobLog.getId());
		return "joblog/joblog.detail";
	}

	@RequestMapping("/logDetailCat")
	@ResponseBody
	public ReturnT<LogResult> logDetailCat(String executorAddress, long triggerTime, int logId, int fromLineNum){
		try {
			ExecutorService executorBiz = TimingJobScheduler.getExecutorBiz(executorAddress);
			ReturnT<LogResult> logResult = executorBiz.log(triggerTime, logId, fromLineNum);

			// is end
            if (logResult.getContent()!=null && logResult.getContent().getFromLineNum() > logResult.getContent().getToLineNum()) {
                TimingJobLog jobLog = timingJobLogDao.load(logId);
                if (jobLog.getHandleCode() > 0) {
                    logResult.getContent().setEnd(true);
                }
            }

			return logResult;
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
			return new ReturnT<LogResult>(ReturnT.FAIL_CODE, e.getMessage());
		}
	}

	@RequestMapping("/logKill")
	@ResponseBody
	public ReturnT<String> logKill(int id){
		// base check
		TimingJobLog log = timingJobLogDao.load(id);
		TimingJobInfo jobInfo = timingJobInfoDao.loadById(log.getJobId());
		if (jobInfo==null) {
			return new ReturnT<String>(500, "参数异常");
		}
		if (ReturnT.SUCCESS_CODE != log.getTriggerCode()) {
			return new ReturnT<String>(500, "调度失败，无法终止日志");
		}

		// request of kill
		ReturnT<String> runResult = null;
		try {
			ExecutorService executorBiz = TimingJobScheduler.getExecutorBiz(log.getExecutorAddress());
			runResult = executorBiz.kill(jobInfo.getId());
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
			runResult = new ReturnT<String>(500, e.getMessage());
		}

		if (ReturnT.SUCCESS_CODE == runResult.getCode()) {
			log.setHandleCode(ReturnT.FAIL_CODE);
			log.setHandleMsg("人为操作主动终止:" + (runResult.getMsg()!=null?runResult.getMsg():""));
			log.setHandleTime(new Date());
			timingJobLogDao.updateHandleInfo(log);
			return new ReturnT<String>(runResult.getMsg());
		} else {
			return new ReturnT<String>(500, runResult.getMsg());
		}
	}

	@RequestMapping("/clearLog")
	@ResponseBody
	public ReturnT<String> clearLog(int jobGroup, int jobId, int type){

		Date clearBeforeTime = null;
		int clearBeforeNum = 0;
		if (type == 1) {
			clearBeforeTime = DateUtils.addMonths(new Date(), -1);	// 清理一个月之前日志数据
		} else if (type == 2) {
			clearBeforeTime = DateUtils.addMonths(new Date(), -3);	// 清理三个月之前日志数据
		} else if (type == 3) {
			clearBeforeTime = DateUtils.addMonths(new Date(), -6);	// 清理六个月之前日志数据
		} else if (type == 4) {
			clearBeforeTime = DateUtils.addYears(new Date(), -1);	// 清理一年之前日志数据
		} else if (type == 5) {
			clearBeforeNum = 1000;		// 清理一千条以前日志数据
		} else if (type == 6) {
			clearBeforeNum = 10000;		// 清理一万条以前日志数据
		} else if (type == 7) {
			clearBeforeNum = 30000;		// 清理三万条以前日志数据
		} else if (type == 8) {
			clearBeforeNum = 100000;	// 清理十万条以前日志数据
		} else if (type == 9) {
			clearBeforeNum = 0;			// 清理所用日志数据
		} else {
			return new ReturnT<String>(ReturnT.FAIL_CODE, "清理类型参数异常");
		}

		timingJobLogDao.clearLog(jobGroup, jobId, clearBeforeTime, clearBeforeNum);
		return ReturnT.SUCCESS;
	}

}
