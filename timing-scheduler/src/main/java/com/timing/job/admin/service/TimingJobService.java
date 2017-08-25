package com.timing.job.admin.service;


import com.timing.executor.core.biz.model.ReturnT;
import com.timing.job.admin.core.model.TimingJobInfo;

import java.util.Map;

public interface TimingJobService {
	
	public Map<String, Object> pageList(int start, int length, int jobGroup, String executorHandler, String filterTime);
	
	public ReturnT<String> add(TimingJobInfo jobInfo);
	
	public ReturnT<String> reschedule(TimingJobInfo jobInfo);
	
	public ReturnT<String> remove(int id);
	
	public ReturnT<String> pause(int id);
	
	public ReturnT<String> resume(int id);
	
	public ReturnT<String> triggerJob(int id);

	public Map<String,Object> dashboardInfo();

	public ReturnT<Map<String,Object>> triggerChartDate();

}
