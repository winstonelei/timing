package com.timing.job.admin.core.route.strategy;

import com.timing.executor.core.biz.ExecutorService;
import com.timing.executor.core.biz.model.ReturnT;
import com.timing.executor.core.biz.model.TriggerParam;
import com.timing.job.admin.core.route.ExecutorRouter;
import com.timing.job.admin.core.schedule.TimingJobScheduler;
import com.timing.job.admin.core.trigger.TimingJobTrigger;

import java.util.ArrayList;

/**
 * Created by winstone on 2017/8/23.
 */
public class ExecutorRouteFailover extends ExecutorRouter {


    public String route(int jobId, ArrayList<String> addressList) {
        return addressList.get(0);
    }

    @Override
    public ReturnT<String> routeRun(TriggerParam triggerParam, ArrayList<String> addressList) {

        StringBuffer beatResultSB = new StringBuffer();
        for (String address : addressList) {
            // beat
            ReturnT<String> beatResult = null;
            try {
                ExecutorService executorBiz = TimingJobScheduler.getExecutorBiz(address);
                beatResult = executorBiz.beat();
            } catch (Exception e) {
                logger.error(e.getMessage(), e);
                beatResult = new ReturnT<String>(ReturnT.FAIL_CODE, ""+e );
            }
            beatResultSB.append( (beatResultSB.length()>0)?"<br><br>":"")
                    .append("心跳检测：")
                    .append("<br>address：").append(address)
                    .append("<br>code：").append(beatResult.getCode())
                    .append("<br>msg：").append(beatResult.getMsg());

            // beat success
            if (beatResult.getCode() == ReturnT.SUCCESS_CODE) {

                ReturnT<String> runResult = TimingJobTrigger.runExecutor(triggerParam, address);
                beatResultSB.append("<br><br>").append(runResult.getMsg());

                // result
                runResult.setMsg(beatResultSB.toString());
                runResult.setContent(address);
                return runResult;
            }
        }
        return new ReturnT<String>(ReturnT.FAIL_CODE, beatResultSB.toString());

    }

}
