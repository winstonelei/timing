package com.timing.job.admin.dao;

import com.timing.job.admin.core.model.TimingJobLogGlue;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * job log for glue
 */
public interface TimingJobLogGlueDao {
	
	public int save(TimingJobLogGlue jobLogGlue);
	
	public List<TimingJobLogGlue> findByJobId(@Param("jobId") int jobId);

	public int removeOld(@Param("jobId") int jobId, @Param("limit") int limit);

	public int deleteByJobId(@Param("jobId") int jobId);
	
}
