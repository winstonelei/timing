package com.timing.executor.core.biz;

import com.timing.executor.core.biz.model.LogResult;
import com.timing.executor.core.biz.model.ReturnT;
import com.timing.executor.core.biz.model.TriggerParam;

/**
 * Created by winstone on 2017/8/18.
 */
public interface ExecutorService {


    /**
     * beat 心跳
     * @return
     */
    public ReturnT<String> beat();


    public ReturnT<String> idleBeat(int jobId);


    /**
     * kill 掉某个job
     * @param jobId
     * @return
     */
    public ReturnT<String> kill(int jobId);


    /**
     * 记录掉某个日志
     * @param logDateTim
     * @param logId
     * @param fromLineNum
     * @return
     */
    public ReturnT<LogResult> log(long logDateTim, int logId, int fromLineNum);


    /**
     * 执行某个任务
     * @param triggerParam
     * @return
     */
    public ReturnT<String> run(TriggerParam triggerParam);

}
