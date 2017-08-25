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
public class ExecutorRouteBusyover extends ExecutorRouter {


    public String route(int jobId, ArrayList<String> addressList) {
        return addressList.get(0);
    }

    @Override
    public ReturnT<String> routeRun(TriggerParam triggerParam, ArrayList<String> addressList) {
        StringBuffer idleBeatResultSB = new StringBuffer();
        for (String address : addressList) {
            // beat
            ReturnT<String> idleBeatResult = null;
            try {
                ExecutorService executorBiz = TimingJobScheduler.getExecutorBiz(address);
                idleBeatResult = executorBiz.idleBeat(triggerParam.getJobId());
            } catch (Exception e) {
                logger.error(e.getMessage(), e);
                idleBeatResult = new ReturnT<String>(ReturnT.FAIL_CODE, ""+e );
            }
            idleBeatResultSB.append( (idleBeatResultSB.length()>0)?"<br><br>":"")
                    .append("空闲检测：")
                    .append("<br>address：").append(address)
                    .append("<br>code：").append(idleBeatResult.getCode())
                    .append("<br>msg：").append(idleBeatResult.getMsg());

            // beat success
            if (idleBeatResult.getCode() == ReturnT.SUCCESS_CODE) {

                ReturnT<String> runResult = TimingJobTrigger.runExecutor(triggerParam, address);
                idleBeatResultSB.append("<br><br>").append(runResult.getMsg());

                // result
                runResult.setMsg(idleBeatResultSB.toString());
                runResult.setContent(address);
                return runResult;
            }
        }

        return new ReturnT<String>(ReturnT.FAIL_CODE, idleBeatResultSB.toString());
    }
}
