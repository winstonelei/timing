package com.timing.job.admin.core.trigger;

import com.timing.executor.core.biz.ExecutorService;
import com.timing.executor.core.biz.model.ExecutorBlockStrategyEnum;
import com.timing.executor.core.biz.model.ReturnT;
import com.timing.executor.core.biz.model.TriggerParam;
import com.timing.job.admin.core.enums.ExecutorFailStrategyEnum;
import com.timing.job.admin.core.model.TimingJobGroup;
import com.timing.job.admin.core.model.TimingJobInfo;
import com.timing.job.admin.core.model.TimingJobLog;
import com.timing.job.admin.core.route.ExecutorRouteStrategyEnum;
import com.timing.job.admin.core.schedule.TimingJobScheduler;
import com.timing.job.admin.core.thread.JobFailMonitorHelper;
import org.apache.commons.collections.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Date;

/**
 * Created by winstone on 2017/8/23.
 */
public class TimingJobTrigger {

    private static Logger logger = LoggerFactory.getLogger(TimingJobTrigger.class);

    public static void trigger(int jobId) {
        // load data
        TimingJobInfo jobInfo = TimingJobScheduler.timingJobInfoDao.loadById(jobId);              // job info
        TimingJobGroup group = TimingJobScheduler.timingJobGroupDao.load(jobInfo.getJobGroup());  // group info

        //TimingJobScheduler.timingJobRegistryDao.


        ExecutorBlockStrategyEnum blockStrategy = ExecutorBlockStrategyEnum.match(jobInfo.getExecutorBlockStrategy(), ExecutorBlockStrategyEnum.SERIAL_EXECUTION);  // block strategy
        ExecutorFailStrategyEnum failStrategy = ExecutorFailStrategyEnum.match(jobInfo.getExecutorFailStrategy(), ExecutorFailStrategyEnum.FAIL_ALARM);    // fail strategy
        ExecutorRouteStrategyEnum executorRouteStrategyEnum = ExecutorRouteStrategyEnum.match(jobInfo.getExecutorRouteStrategy(), null);    // route strategy
        ArrayList<String> addressList = null;
        if(null!=group){
            addressList = (ArrayList<String>) group.getRegistryList();
        }



        // broadcast
        if (ExecutorRouteStrategyEnum.SHARDING_BROADCAST == executorRouteStrategyEnum && CollectionUtils.isNotEmpty(addressList)) {
            for (int i = 0; i < addressList.size(); i++) {
                String address = addressList.get(i);

                // 1、save log-id
                TimingJobLog jobLog = new TimingJobLog();
                jobLog.setJobGroup(jobInfo.getJobGroup());
                jobLog.setJobId(jobInfo.getId());
                TimingJobScheduler.timingJobLogDao.save(jobLog);
                logger.debug(">>>>>>>>>>> timing-job trigger start, jobId:{}", jobLog.getId());

                // 2、prepare trigger-info
                //jobLog.setExecutorAddress(executorAddress);
                jobLog.setGlueType(jobInfo.getGlueType());
                jobLog.setExecutorHandler(jobInfo.getExecutorHandler());
                jobLog.setExecutorParam(jobInfo.getExecutorParam());
                jobLog.setTriggerTime(new Date());

                ReturnT<String> triggerResult = new ReturnT<String>(null);
                StringBuffer triggerMsgSb = new StringBuffer();
                triggerMsgSb.append("注册方式：").append( (group.getAddressType() == 0)?"自动注册":"手动录入" );
                triggerMsgSb.append("<br>阻塞处理策略：").append(blockStrategy.getTitle());
                triggerMsgSb.append("<br>失败处理策略：").append(failStrategy.getTitile());
                triggerMsgSb.append("<br>地址列表：").append(group.getRegistryList());
                triggerMsgSb.append("<br>路由策略：").append(executorRouteStrategyEnum.getTitle()).append("("+i+"/"+addressList.size()+")"); // update01

                // 3、trigger-valid
                if (triggerResult.getCode()==ReturnT.SUCCESS_CODE && CollectionUtils.isEmpty(addressList)) {
                    triggerResult.setCode(ReturnT.FAIL_CODE);
                    triggerMsgSb.append("<br>----------------------<br>").append("调度失败：").append("执行器地址为空");
                }

                if (triggerResult.getCode() == ReturnT.SUCCESS_CODE) {
                    // 4.1、trigger-param
                    TriggerParam triggerParam = new TriggerParam();
                    triggerParam.setJobId(jobInfo.getId());
                    triggerParam.setExecutorHandler(jobInfo.getExecutorHandler());
                    triggerParam.setExecutorParams(jobInfo.getExecutorParam());
                    triggerParam.setExecutorBlockStrategy(jobInfo.getExecutorBlockStrategy());
                    triggerParam.setLogId(jobLog.getId());
                    triggerParam.setLogDateTim(jobLog.getTriggerTime().getTime());
                    triggerParam.setGlueType(jobInfo.getGlueType());
                    triggerParam.setGlueSource(jobInfo.getGlueSource());
                    triggerParam.setGlueUpdatetime(jobInfo.getGlueUpdatetime().getTime());
                    triggerParam.setBroadcastIndex(i);
                    triggerParam.setBroadcastTotal(addressList.size()); // update02

                    // 4.2、trigger-run (route run / trigger remote executor)
                    triggerResult = runExecutor(triggerParam, address);     // update03
                    triggerMsgSb.append("<br><br><span style=\"color:#00c0ef;\" > >>>>>>>>>>>触发调度<<<<<<<<<<< </span><br>").append(triggerResult.getMsg());

                    // 4.3、trigger (fail retry)
                    if (triggerResult.getCode()!=ReturnT.SUCCESS_CODE && failStrategy == ExecutorFailStrategyEnum.FAIL_RETRY) {
                        triggerResult = runExecutor(triggerParam, address);  // update04
                        triggerMsgSb.append("<br><br><span style=\"color:#F39C12;\" > >>>>>>>>>>>失败重试<<<<<<<<<<< </span><br>").append(triggerResult.getMsg());
                    }
                }

                // 5、save trigger-info
                jobLog.setExecutorAddress(triggerResult.getContent());
                jobLog.setTriggerCode(triggerResult.getCode());
                jobLog.setTriggerMsg(triggerMsgSb.toString());
                TimingJobScheduler.timingJobLogDao.updateTriggerInfo(jobLog);

                // 6、monitor triger
                JobFailMonitorHelper.monitor(jobLog.getId());
                logger.debug(">>>>>>>>>>> timing-job trigger end, jobId:{}", jobLog.getId());

            }
            return;
        }

        // 1、save log-id
        TimingJobLog jobLog = new TimingJobLog();
        jobLog.setJobGroup(jobInfo.getJobGroup());
        jobLog.setJobId(jobInfo.getId());
        TimingJobScheduler.timingJobLogDao.save(jobLog);
        logger.debug(">>>>>>>>>>> timing-job trigger start, jobId:{}", jobLog.getId());

        // 2、prepare trigger-info
        //jobLog.setExecutorAddress(executorAddress);
        jobLog.setGlueType(jobInfo.getGlueType());
        jobLog.setExecutorHandler(jobInfo.getExecutorHandler());
        jobLog.setExecutorParam(jobInfo.getExecutorParam());
        jobLog.setTriggerTime(new Date());

        ReturnT<String> triggerResult = new ReturnT<String>(null);
        StringBuffer triggerMsgSb = new StringBuffer();
        triggerMsgSb.append("注册方式：").append( (group.getAddressType() == 0)?"自动注册":"手动录入" );
        triggerMsgSb.append("<br>阻塞处理策略：").append(blockStrategy.getTitle());
        triggerMsgSb.append("<br>失败处理策略：").append(failStrategy.getTitile());
        triggerMsgSb.append("<br>地址列表：").append(group.getRegistryList());
        triggerMsgSb.append("<br>路由策略：").append(executorRouteStrategyEnum.getTitle());

        // 3、trigger-valid
        if (triggerResult.getCode()==ReturnT.SUCCESS_CODE && CollectionUtils.isEmpty(addressList)) {
            triggerResult.setCode(ReturnT.FAIL_CODE);
            triggerMsgSb.append("<br>----------------------<br>").append("调度失败：").append("执行器地址为空");
        }

        if (triggerResult.getCode() == ReturnT.SUCCESS_CODE) {
            // 4.1、trigger-param
            TriggerParam triggerParam = new TriggerParam();
            triggerParam.setJobId(jobInfo.getId());
            triggerParam.setExecutorHandler(jobInfo.getExecutorHandler());
            triggerParam.setExecutorParams(jobInfo.getExecutorParam());
            triggerParam.setExecutorBlockStrategy(jobInfo.getExecutorBlockStrategy());
            triggerParam.setLogId(jobLog.getId());
            triggerParam.setLogDateTim(jobLog.getTriggerTime().getTime());
            triggerParam.setGlueType(jobInfo.getGlueType());
            triggerParam.setGlueSource(jobInfo.getGlueSource());
            triggerParam.setGlueUpdatetime(jobInfo.getGlueUpdatetime().getTime());
            triggerParam.setBroadcastIndex(0);
            triggerParam.setBroadcastTotal(1);

            // 4.2、trigger-run (route run / trigger remote executor)
            triggerResult = executorRouteStrategyEnum.getRouter().routeRun(triggerParam, addressList);
            triggerMsgSb.append("<br><br><span style=\"color:#00c0ef;\" > >>>>>>>>>>>触发调度<<<<<<<<<<< </span><br>").append(triggerResult.getMsg());

            // 4.3、trigger (fail retry)
            if (triggerResult.getCode()!=ReturnT.SUCCESS_CODE && failStrategy == ExecutorFailStrategyEnum.FAIL_RETRY) {
                triggerResult = executorRouteStrategyEnum.getRouter().routeRun(triggerParam, addressList);
                triggerMsgSb.append("<br><br><span style=\"color:#F39C12;\" > >>>>>>>>>>>失败重试<<<<<<<<<<< </span><br>").append(triggerResult.getMsg());
            }
        }

        // 5、save trigger-info
        jobLog.setExecutorAddress(triggerResult.getContent());
        jobLog.setTriggerCode(triggerResult.getCode());
        jobLog.setTriggerMsg(triggerMsgSb.toString());
        TimingJobScheduler.timingJobLogDao.updateTriggerInfo(jobLog);

        // 6、monitor triger
        JobFailMonitorHelper.monitor(jobLog.getId());
        logger.debug(">>>>>>>>>>> timing-job trigger end, jobId:{}", jobLog.getId());


    }


    public  static ReturnT<String> runExecutor(TriggerParam param,String address){
        ReturnT<String> runResult = null;
        try {
            ExecutorService executorBiz = TimingJobScheduler.getExecutorBiz(address);
            runResult = executorBiz.run(param);
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            runResult = new ReturnT<String>(ReturnT.FAIL_CODE, ""+e );
        }

        StringBuffer runResultSB = new StringBuffer("触发调度：");
        runResultSB.append("<br>address：").append(address);
        runResultSB.append("<br>code：").append(runResult.getCode());
        runResultSB.append("<br>msg：").append(runResult.getMsg());

        runResult.setMsg(runResultSB.toString());
        runResult.setContent(address);
        return runResult;

    }

}
