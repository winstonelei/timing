package com.timing.job.admin.core.route.strategy;


import com.timing.executor.core.biz.model.ReturnT;
import com.timing.executor.core.biz.model.TriggerParam;
import com.timing.job.admin.core.route.ExecutorRouter;
import com.timing.job.admin.core.trigger.TimingJobTrigger;

import java.util.ArrayList;

public class ExecutorRouteLast extends ExecutorRouter {

    public String route(int jobId, ArrayList<String> addressList) {
        return addressList.get(addressList.size()-1);
    }

    @Override
    public ReturnT<String> routeRun(TriggerParam triggerParam, ArrayList<String> addressList) {
        // address
        String address = route(triggerParam.getJobId(), addressList);

        // run executor
        ReturnT<String> runResult = TimingJobTrigger.runExecutor(triggerParam, address);
        runResult.setContent(address);
        return runResult;
    }
}
