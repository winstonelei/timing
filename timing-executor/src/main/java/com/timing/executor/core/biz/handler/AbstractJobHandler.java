package com.timing.executor.core.biz.handler;

import com.timing.executor.core.biz.model.ReturnT;

/**
 * Created by winstone on 2017/8/21.
 */
public abstract class AbstractJobHandler {

    public abstract ReturnT<String> execute(String... params)throws  Exception;

}
