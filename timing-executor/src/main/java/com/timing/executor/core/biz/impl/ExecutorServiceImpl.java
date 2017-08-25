package com.timing.executor.core.biz.impl;

import com.timing.executor.core.biz.ExecutorService;
import com.timing.executor.core.biz.executor.TimingJobExecutor;
import com.timing.executor.core.biz.groovy.GlueFactory;
import com.timing.executor.core.biz.groovy.GlueTypeEnum;
import com.timing.executor.core.biz.handler.AbstractJobHandler;
import com.timing.executor.core.biz.handler.impl.GlueJobHandler;
import com.timing.executor.core.biz.handler.impl.ScriptJobExecuteHandler;
import com.timing.executor.core.biz.log.TimingJobFileAppender;
import com.timing.executor.core.biz.model.ExecutorBlockStrategyEnum;
import com.timing.executor.core.biz.model.LogResult;
import com.timing.executor.core.biz.model.ReturnT;
import com.timing.executor.core.biz.model.TriggerParam;
import com.timing.executor.core.biz.thread.JobThread;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Date;

/**
 * Created by winstone on 2017/8/18.
 */
public class ExecutorServiceImpl implements ExecutorService {

    private static Logger logger = LoggerFactory.getLogger(ExecutorServiceImpl.class);

    @Override
    public ReturnT<String> beat() {
        return ReturnT.SUCCESS;
    }

    @Override
    public ReturnT<String> idleBeat(int jobId) {
        boolean isRunningOrHasQueue = false;
        JobThread jobThread = TimingJobExecutor.loadJobThread(jobId);
        if (jobThread != null && jobThread.isRunningOrHasQueue()) {
            isRunningOrHasQueue = true;
        }

        if (isRunningOrHasQueue) {
            return new ReturnT<String>(ReturnT.FAIL_CODE, "job thread is running or has trigger queue.");
        }
        return ReturnT.SUCCESS;
    }

    @Override
    public ReturnT<String> kill(int jobId) {
        JobThread jobThread = TimingJobExecutor.loadJobThread(jobId);
        if (jobThread != null) {
            TimingJobExecutor.removeJobThread(jobId, "人工手动终止");
            return ReturnT.SUCCESS;
        }
        return new ReturnT<String>(ReturnT.SUCCESS_CODE, "job thread aleady killed.");
    }

    @Override
    public ReturnT<LogResult> log(long logDateTim, int logId, int fromLineNum) {
        String logFileName = TimingJobFileAppender.makeLogFileName(new Date(logDateTim), logId);
        LogResult logResult = TimingJobFileAppender.readLog(logFileName, fromLineNum);
        return new ReturnT<LogResult>(logResult);
    }

    @Override
    public ReturnT<String> run(TriggerParam triggerParam) {
        //加载oldJobThread oldJobHandler
        JobThread jobThread = TimingJobExecutor.loadJobThread(triggerParam.getJobId());
        AbstractJobHandler jobHandler =  jobThread!=null?jobThread.getHandler():null;
        String removeReason = null;
        if(GlueTypeEnum.BEAN == GlueTypeEnum.match(triggerParam.getGlueType())){
            AbstractJobHandler newJobHandler = TimingJobExecutor.loadJobHandler(triggerParam.getExecutorHandler());
            if(jobThread!=null && jobHandler != newJobHandler){
                removeReason ="更换JobHandler或更换任务模式,终止旧任务线程";
                jobHandler = null;
                jobThread = null;
            }
            if(jobHandler == null){
                jobHandler = newJobHandler;
                if(jobHandler == null){
                    return new ReturnT<String>(ReturnT.FAIL_CODE, "job handler [" + triggerParam.getExecutorHandler() + "] not found.");
                }
            }
        }else if(GlueTypeEnum.GLUE_GROOVY==GlueTypeEnum.match(triggerParam.getGlueType())){

            if (jobThread != null &&
                    !(jobThread.getHandler() instanceof GlueJobHandler
                            && ((GlueJobHandler) jobThread.getHandler()).getGlueUpdatetime()==triggerParam.getGlueUpdatetime() )) {
                // change handler or gluesource updated, need kill old thread
                removeReason = "更新任务逻辑或更换任务模式,终止旧任务线程";

                jobThread = null;
                jobHandler = null;
            }

            // valid handler
            if (jobHandler == null) {
                try {
                    AbstractJobHandler originJobHandler = GlueFactory.getInstance().loadNewInstance(triggerParam.getGlueSource());
                    jobHandler = new GlueJobHandler(originJobHandler, triggerParam.getGlueUpdatetime());
                } catch (Exception e) {
                    logger.error("", e);
                    return new ReturnT<String>(ReturnT.FAIL_CODE, e.getMessage());
                }
            }

        }else if(GlueTypeEnum.GLUE_SHELL==GlueTypeEnum.match(triggerParam.getGlueType())
                || GlueTypeEnum.GLUE_PYTHON==GlueTypeEnum.match(triggerParam.getGlueType())){

            // valid old jobThread
            if (jobThread != null &&
                    !(jobThread.getHandler() instanceof ScriptJobExecuteHandler
                            && ((ScriptJobExecuteHandler) jobThread.getHandler()).getGlueUpdatetime()==triggerParam.getGlueUpdatetime() )) {
                // change script or gluesource updated, need kill old thread
                removeReason = "更新任务逻辑或更换任务模式,终止旧任务线程";

                jobThread = null;
                jobHandler = null;
            }

            // valid handler
            if (jobHandler == null) {
                jobHandler = new ScriptJobExecuteHandler(triggerParam.getJobId(), triggerParam.getGlueUpdatetime(), triggerParam.getGlueSource(), GlueTypeEnum.match(triggerParam.getGlueType()));
            }

        }else{
            return new ReturnT<String>(ReturnT.FAIL_CODE, "glueType[" + triggerParam.getGlueType() + "] is not valid.");
        }

        // executor block strategy
        if (jobThread != null) {
            ExecutorBlockStrategyEnum blockStrategy = ExecutorBlockStrategyEnum.match(triggerParam.getExecutorBlockStrategy(), null);
            if (ExecutorBlockStrategyEnum.DISCARD_LATER == blockStrategy) {
                // discard when running
                if (jobThread.isRunningOrHasQueue()) {
                    return new ReturnT<String>(ReturnT.FAIL_CODE, "阻塞处理策略-生效："+ExecutorBlockStrategyEnum.DISCARD_LATER.getTitle());
                }
            } else if (ExecutorBlockStrategyEnum.COVER_EARLY == blockStrategy) {
                // kill running jobThread
                if (jobThread.isRunningOrHasQueue()) {
                    removeReason = "阻塞处理策略-生效：" + ExecutorBlockStrategyEnum.COVER_EARLY.getTitle();
                    jobThread = null;
                }
            } else {
                // just queue trigger
            }
        }

        // replace thread (new or exists invalid)
        if (jobThread == null) {
            jobThread = TimingJobExecutor.registJobThread(triggerParam.getJobId(), jobHandler, removeReason);
        }

        // push data to queue
        ReturnT<String> pushResult = jobThread.pushTriggerQueue(triggerParam);
        return pushResult;
    }
}
