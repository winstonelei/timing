package com.timing.executor.core.biz.handler.impl;

import com.timing.executor.core.biz.handler.AbstractJobHandler;
import com.timing.executor.core.biz.model.ReturnT;

/**
 * Created by winstone on 2017/8/21.
 */
public class GlueJobHandler extends AbstractJobHandler {

    private long glueUpdatetime;
    private AbstractJobHandler jobHandler;

    public GlueJobHandler(AbstractJobHandler jobHandler, long glueUpdatetime){
        this.jobHandler = jobHandler;
        this.glueUpdatetime = glueUpdatetime;
    }

    public long getGlueUpdatetime(){return glueUpdatetime;}

    @Override
    public ReturnT<String> execute(String... params) throws Exception {
        return jobHandler.execute(params);
    }
}
