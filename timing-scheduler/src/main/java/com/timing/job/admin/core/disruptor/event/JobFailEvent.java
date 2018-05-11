package com.timing.job.admin.core.disruptor.event;

import java.io.Serializable;

/**
 * job 任务失败事件
 */
public class JobFailEvent implements Serializable{

    private Integer jobId;

    public Integer getJobId() {
        return jobId;
    }

    public void setJobId(Integer jobId) {
        this.jobId = jobId;
    }
}
