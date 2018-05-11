package com.timing.job.admin.core.disruptor.translator;

import com.lmax.disruptor.EventTranslatorOneArg;
import com.timing.executor.core.biz.disruptor.event.HandleCallbackEvent;
import com.timing.executor.core.biz.model.HandleCallbackParam;
import com.timing.job.admin.core.disruptor.event.JobFailEvent;

/**
  事件转换器translator
 */
public class JobFailEventTranslator implements EventTranslatorOneArg<JobFailEvent,Integer> {

    @Override
    public void translateTo(JobFailEvent event, long sequence, Integer jobId) {
        event.setJobId(jobId);
    }
}
