package com.timing.job.admin.controller;

import com.timing.executor.core.biz.groovy.GlueTypeEnum;
import com.timing.executor.core.biz.model.ReturnT;
import com.timing.job.admin.core.model.TimingJobInfo;
import com.timing.job.admin.core.model.TimingJobLogGlue;
import com.timing.job.admin.dao.TimingJobInfoDao;
import com.timing.job.admin.dao.TimingJobLogGlueDao;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.annotation.Resource;
import java.util.Date;
import java.util.List;

@Controller
@RequestMapping("/jobcode")
public class JobCodeController {
	
	@Resource
	private TimingJobInfoDao timingJobInfoDao;
	@Resource
	private TimingJobLogGlueDao  timingJobLogGlueDao;

	@RequestMapping
	public String index(Model model, int jobId) {
		TimingJobInfo jobInfo = timingJobInfoDao.loadById(jobId);
		List<TimingJobLogGlue> jobLogGlues = timingJobLogGlueDao.findByJobId(jobId);

		if (jobInfo == null) {
			throw new RuntimeException("抱歉，任务不存在.");
		}
		if (GlueTypeEnum.BEAN == GlueTypeEnum.match(jobInfo.getGlueType())) {
			throw new RuntimeException("该任务非GLUE模式.");
		}

		// Glue类型-字典
		model.addAttribute("GlueTypeEnum", GlueTypeEnum.values());

		model.addAttribute("jobInfo", jobInfo);
		model.addAttribute("jobLogGlues", jobLogGlues);
		return "jobcode/jobcode.index";
	}
	
	@RequestMapping("/save")
	@ResponseBody
	public ReturnT<String> save(Model model, int id, String glueSource, String glueRemark) {
		// valid
		if (glueRemark==null) {
			return new ReturnT<String>(500, "请输入备注");
		}
		if (glueRemark.length()<4 || glueRemark.length()>100) {
			return new ReturnT<String>(500, "备注长度应该在4至100之间");
		}
		TimingJobInfo exists_jobInfo = timingJobInfoDao.loadById(id);
		if (exists_jobInfo == null) {
			return new ReturnT<String>(500, "参数异常");
		}
		
		// update new code
		exists_jobInfo.setGlueSource(glueSource);
		exists_jobInfo.setGlueRemark(glueRemark);
		exists_jobInfo.setGlueUpdatetime(new Date());
		timingJobInfoDao.update(exists_jobInfo);

		// log old code
		TimingJobLogGlue xxlJobLogGlue = new TimingJobLogGlue();
		xxlJobLogGlue.setJobId(exists_jobInfo.getId());
		xxlJobLogGlue.setGlueType(exists_jobInfo.getGlueType());
		xxlJobLogGlue.setGlueSource(glueSource);
		xxlJobLogGlue.setGlueRemark(glueRemark);
		timingJobLogGlueDao.save(xxlJobLogGlue);

		// remove code backup more than 30
		timingJobLogGlueDao.removeOld(exists_jobInfo.getId(), 30);

		return ReturnT.SUCCESS;
	}
	
}
