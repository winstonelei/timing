package com.timing.job.admin.core.jobbean;

import com.timing.job.admin.core.trigger.TimingJobTrigger;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.JobKey;
import org.springframework.scheduling.quartz.QuartzJobBean;

/**
 * Created by winstone on 2017/8/23.
 */
public class RemoteHttpJobBean extends QuartzJobBean {

    @Override
    protected void executeInternal(JobExecutionContext jobExecutionContext) throws JobExecutionException {

        //加载jobkey jobId
        JobKey  jobKey = jobExecutionContext.getTrigger().getJobKey();
        Integer jobId = Integer.valueOf(jobKey.getName());

        TimingJobTrigger.trigger(jobId);

    }
}
