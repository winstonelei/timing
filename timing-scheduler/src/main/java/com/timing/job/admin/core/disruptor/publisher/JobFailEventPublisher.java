package com.timing.job.admin.core.disruptor.publisher;

import com.lmax.disruptor.RingBuffer;
import com.lmax.disruptor.YieldingWaitStrategy;
import com.lmax.disruptor.dsl.Disruptor;
import com.lmax.disruptor.dsl.ProducerType;
import com.timing.executor.core.biz.disruptor.event.HandleCallbackEvent;
import com.timing.executor.core.biz.disruptor.translator.HandleCallbackEventTranslator;
import com.timing.executor.core.biz.model.HandleCallbackParam;
import com.timing.job.admin.core.disruptor.event.JobFailEvent;
import com.timing.job.admin.core.disruptor.factory.JobFailEventFactory;
import com.timing.job.admin.core.disruptor.handler.JobFailEventHandler;
import com.timing.job.admin.core.disruptor.translator.JobFailEventTranslator;

import java.util.concurrent.atomic.AtomicInteger;

/**
   失败发布事件，发送器
 */
public class JobFailEventPublisher {

    private JobFailEventPublisher(){}

    private static class JobFailEventPubliserHoder{
        static final  JobFailEventPublisher instance = new JobFailEventPublisher();
    }

    public static JobFailEventPublisher getInstance(){
        return JobFailEventPubliserHoder.instance;
    }

    private Disruptor<JobFailEvent> disruptor;

    private JobFailEventHandler jobFailEventHandler;

    public void start(int bufferSize){
        jobFailEventHandler = JobFailEventHandler.getInstance();
        disruptor = new Disruptor<JobFailEvent>(new JobFailEventFactory(),bufferSize, r->{
            AtomicInteger index = new AtomicInteger(1);
            return new Thread(null, r, "disruptor-thread-" + index.getAndIncrement());}, ProducerType.MULTI,new YieldingWaitStrategy());
        disruptor.handleEventsWith(jobFailEventHandler);
        disruptor.start();
    }

    public void  publishEvent(Integer failJobId){
        final RingBuffer<JobFailEvent> ringBuffer = disruptor.getRingBuffer();
        ringBuffer.publishEvent(new JobFailEventTranslator(),failJobId);
    }

    public void destory(){
        disruptor.shutdown();
    }




}
