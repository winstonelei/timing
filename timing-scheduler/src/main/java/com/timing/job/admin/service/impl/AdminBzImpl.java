package com.timing.job.admin.service.impl;

import com.timing.executor.core.biz.AdminBz;
import com.timing.executor.core.biz.model.HandleCallbackParam;
import com.timing.executor.core.biz.model.RegistryParam;
import com.timing.executor.core.biz.model.ReturnT;
import com.timing.job.admin.core.model.TimingJobInfo;
import com.timing.job.admin.core.model.TimingJobLog;
import com.timing.job.admin.core.schedule.TimingJobScheduler;
import com.timing.job.admin.dao.TimingJobInfoDao;
import com.timing.job.admin.dao.TimingJobLogDao;
import com.timing.job.admin.dao.TimingJobRegistryDao;
import org.apache.commons.lang.StringUtils;
import org.quartz.SchedulerException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.text.MessageFormat;
import java.util.Date;
import java.util.List;

/**
 * Created by winstone on 2017/8/23.
 */
@Service
public class AdminBzImpl implements AdminBz{


    private static Logger logger = LoggerFactory.getLogger(AdminBzImpl.class);

    @Resource
    public TimingJobLogDao timingJobLogDao;
    @Resource
    private TimingJobInfoDao timingJobInfoDao;
    @Resource
    private TimingJobRegistryDao timingJobRegistryDao;

    @Override
    public ReturnT<String> callback(List<HandleCallbackParam> callbackParamList) {
        try {
            for (HandleCallbackParam handleCallbackParam: callbackParamList) {
                ReturnT<String> callbackResult = callback(handleCallbackParam);
                logger.info("JobApiController.callback {}, handleCallbackParam={}, callbackResult={}",
                        (callbackResult.getCode()==ReturnT.SUCCESS_CODE?"success":"fail"), handleCallbackParam, callbackResult);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return ReturnT.SUCCESS;
    }

    private ReturnT<String> callback(HandleCallbackParam handleCallbackParam)throws Exception {
        // valid log item
        TimingJobLog log = timingJobLogDao.load(handleCallbackParam.getLogId());
        if (log == null) {
            return new ReturnT<String>(ReturnT.FAIL_CODE, "log item not found.");
        }

        // trigger success, to trigger child job, and avoid repeat trigger child job
        String childTriggerMsg = null;
        if (ReturnT.SUCCESS_CODE==handleCallbackParam.getExecuteResult().getCode() && ReturnT.SUCCESS_CODE!=log.getHandleCode()) {
            TimingJobInfo xxlJobInfo = timingJobInfoDao.loadById(log.getJobId());
            if (xxlJobInfo!=null && StringUtils.isNotBlank(xxlJobInfo.getChildJobKey())) {
                childTriggerMsg = "<hr>";
                String[] childJobKeys = xxlJobInfo.getChildJobKey().split(",");
                for (int i = 0; i < childJobKeys.length; i++) {
                    String[] jobKeyArr = childJobKeys[i].split("_");
                    if (jobKeyArr!=null && jobKeyArr.length==2) {
                        TimingJobInfo childJobInfo = timingJobInfoDao.loadById(Integer.valueOf(jobKeyArr[1]));
                        if (childJobInfo!=null) {
                            try {
                                boolean ret = TimingJobScheduler.triggerJob(String.valueOf(childJobInfo.getId()), String.valueOf(childJobInfo.getJobGroup()));

                                // add msg
                                childTriggerMsg += MessageFormat.format("<br> {0}/{1} 触发子任务成功, 子任务Key: {2}, status: {3}, 子任务描述: {4}",
                                        (i+1), childJobKeys.length, childJobKeys[i], ret, childJobInfo.getJobDesc());
                            } catch (SchedulerException e) {
                                logger.error("", e);
                            }
                        } else {
                            childTriggerMsg += MessageFormat.format("<br> {0}/{1} 触发子任务失败, 子任务xxlJobInfo不存在, 子任务Key: {2}",
                                    (i+1), childJobKeys.length, childJobKeys[i]);
                        }
                    } else {
                        childTriggerMsg += MessageFormat.format("<br> {0}/{1} 触发子任务失败, 子任务Key格式错误, 子任务Key: {2}",
                                (i+1), childJobKeys.length, childJobKeys[i]);
                    }
                }

            }
        }

        // handle msg
        StringBuffer handleMsg = new StringBuffer();
        if (log.getHandleMsg()!=null) {
            handleMsg.append(log.getHandleMsg()).append("<br>");
        }
        if (handleCallbackParam.getExecuteResult().getMsg() != null) {
            handleMsg.append(handleCallbackParam.getExecuteResult().getMsg());
        }
        if (childTriggerMsg !=null) {
            handleMsg.append("<br>子任务触发备注：").append(childTriggerMsg);
        }

        // success, save log
        log.setHandleTime(new Date());
        log.setHandleCode(handleCallbackParam.getExecuteResult().getCode());
        log.setHandleMsg(handleMsg.toString());
        timingJobLogDao.updateHandleInfo(log);

        return ReturnT.SUCCESS;
    }

    @Override
    public ReturnT<String> registry(RegistryParam registryParam) {
        int ret = timingJobRegistryDao.registryUpdate(registryParam.getRegistGroup(), registryParam.getRegistryKey(), registryParam.getRegistryValue());
        if (ret < 1) {
            timingJobRegistryDao.registrySave(registryParam.getRegistGroup(), registryParam.getRegistryKey(), registryParam.getRegistryValue());
        }
        return ReturnT.SUCCESS;
    }
}
