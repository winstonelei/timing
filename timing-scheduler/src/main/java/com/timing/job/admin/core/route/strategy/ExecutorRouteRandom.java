package com.timing.job.admin.core.route.strategy;

import com.timing.executor.core.biz.model.ReturnT;
import com.timing.executor.core.biz.model.TriggerParam;
import com.timing.job.admin.core.route.ExecutorRouter;
import com.timing.job.admin.core.trigger.TimingJobTrigger;

import java.util.ArrayList;
import java.util.Random;

/**
 * Created by winstone on 2017/8/23.
 */
public class ExecutorRouteRandom  extends ExecutorRouter {

    private static Random localRandom = new Random();

    public String route(int jobId, ArrayList<String> addressList) {
        return addressList.get(localRandom.nextInt(addressList.size()));
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
