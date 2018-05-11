package com.timing.executor.core.biz.disruptor.event;

import com.timing.executor.core.biz.model.HandleCallbackParam;

import java.io.Serializable;

public class HandleCallbackEvent implements Serializable{

    private HandleCallbackParam handleCallbackParam;

    public HandleCallbackParam getHandleCallbackParam() {
        return handleCallbackParam;
    }

    public void setHandleCallbackParam(HandleCallbackParam handleCallbackParam) {
        this.handleCallbackParam = handleCallbackParam;
    }
}
