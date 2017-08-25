package com.timing.job.admin.dao;

import com.timing.job.admin.core.model.TimingJobInfo;
import org.apache.ibatis.annotations.Param;

import java.util.List;


/**
 * job info
 */
public interface TimingJobInfoDao {

	public List<TimingJobInfo> pageList(@Param("offset") int offset, @Param("pagesize") int pagesize, @Param("jobGroup") int jobGroup, @Param("executorHandler") String executorHandler);
	public int pageListCount(@Param("offset") int offset, @Param("pagesize") int pagesize, @Param("jobGroup") int jobGroup, @Param("executorHandler") String executorHandler);
	
	public int save(TimingJobInfo info);

	public TimingJobInfo loadById(@Param("id") int id);
	
	public int update(TimingJobInfo item);
	
	public int delete(@Param("id") int id);

	public List<TimingJobInfo> getJobsByGroup(@Param("jobGroup") int jobGroup);

	public int findAllCount();

}
