package com.timing.job.executor.service.handler;

import  com.timing.executor.core.biz.handler.annotation.JobHandler;
import com.timing.executor.core.biz.handler.AbstractJobHandler;
import com.timing.executor.core.biz.log.TimingLogger;
import com.timing.executor.core.biz.model.ReturnT;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * Created by winstone on 2017/8/24.
 */


@JobHandler(value="demoJobHandler")
@Component
public class DemoJobHandler extends AbstractJobHandler {


    @Override
    public ReturnT<String> execute(String... params) throws Exception {
        TimingLogger.log("JOB  working  Hello World.");

        TimingLogger.log("beat at:  adasfdasf " );
        return ReturnT.SUCCESS;
    }
}
