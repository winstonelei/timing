package com.timing.job.admin.core.disruptor.factory;

import com.lmax.disruptor.EventFactory;
import com.timing.job.admin.core.disruptor.event.JobFailEvent;

/**
   失败事件处理工厂
 */
public class JobFailEventFactory  implements EventFactory<JobFailEvent> {

    @Override
    public JobFailEvent newInstance() {
        return new JobFailEvent();
    }
}
