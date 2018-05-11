package com.timing.executor.core.biz.disruptor.publisher;

import com.lmax.disruptor.RingBuffer;
import com.lmax.disruptor.YieldingWaitStrategy;
import com.lmax.disruptor.dsl.Disruptor;
import com.lmax.disruptor.dsl.ProducerType;
import com.timing.executor.core.biz.disruptor.event.HandleCallbackEvent;
import com.timing.executor.core.biz.disruptor.factory.HandleCallbackEventFactory;
import com.timing.executor.core.biz.disruptor.handler.HandleCallbackEventHandler;
import com.timing.executor.core.biz.disruptor.translator.HandleCallbackEventTranslator;
import com.timing.executor.core.biz.model.HandleCallbackParam;

import java.util.concurrent.atomic.AtomicInteger;

/**
   disrupter 发布回调event事件
 */
public class HandleCallbackPublisher {

    private HandleCallbackPublisher(){
    }

    private static class HandleCallbackPublisherHolder{
        static final HandleCallbackPublisher instance = new HandleCallbackPublisher();
    }

    public static HandleCallbackPublisher getInstance(){
        return HandleCallbackPublisherHolder.instance;
    }

    private Disruptor<HandleCallbackEvent> disruptor;

    private HandleCallbackEventHandler handler;

    public void start(int bufferSize){
        handler = HandleCallbackEventHandler.getInstance();
        disruptor = new Disruptor<HandleCallbackEvent>(new HandleCallbackEventFactory(),bufferSize,r->{
                AtomicInteger index = new AtomicInteger(1);
            return new Thread(null, r, "disruptor-thread-" + index.getAndIncrement());}, ProducerType.MULTI,new YieldingWaitStrategy());
        disruptor.handleEventsWith(handler);
        disruptor.start();
    }


    public void  publishEvent(HandleCallbackParam param){
        final RingBuffer<HandleCallbackEvent> ringBuffer = disruptor.getRingBuffer();
        ringBuffer.publishEvent(new HandleCallbackEventTranslator(),param);
    }

    public void destory(){
        disruptor.shutdown();
    }


}
