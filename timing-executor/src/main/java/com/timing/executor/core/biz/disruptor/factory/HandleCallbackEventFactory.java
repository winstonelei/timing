package com.timing.executor.core.biz.disruptor.factory;

import com.lmax.disruptor.EventFactory;
import com.timing.executor.core.biz.disruptor.event.HandleCallbackEvent;


public class HandleCallbackEventFactory  implements EventFactory<HandleCallbackEvent> {
    @Override
    public HandleCallbackEvent newInstance() {
        return new HandleCallbackEvent();
    }

}
