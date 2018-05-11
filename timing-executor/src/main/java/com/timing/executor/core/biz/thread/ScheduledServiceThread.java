package com.timing.executor.core.biz.thread;

import com.timing.executor.core.biz.AdminBz;
import com.timing.executor.core.biz.enums.RegistryConfig;
import com.timing.executor.core.biz.executor.TimingJobExecutor;
import com.timing.executor.core.biz.model.RegistryParam;
import com.timing.executor.core.biz.model.ReturnT;
import com.timing.executor.core.biz.util.IpUtil;
import com.timing.executor.core.biz.util.MyTimingThreadFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
   实现定时调度心跳机制
 */
public class ScheduledServiceThread {

    private static Logger logger = LoggerFactory.getLogger(ExecutorRegistryThread.class);

    private ScheduledServiceThread(){

    }

    private static class ScheduledServiceThreadHolder{
         static  final ScheduledServiceThread scheduledServiceThread = new ScheduledServiceThread();
    }

    public static ScheduledServiceThread getInstance(){
         return ScheduledServiceThreadHolder.scheduledServiceThread;
    }

    public void start(final String ip,final int port,final String appName){
        if (appName==null || appName.trim().length()==0) {
            logger.warn(">>>>>>>>>>>> timing-job, executor registry config fail, appName is null.");
            return;
        }
        if (TimingJobExecutor.getAdminBizList() == null) {
            logger.warn(">>>>>>>>>>>> timing-job, executor registry config fail, adminAddresses is null.");
            return;
        }

        final String executorAddress;
        if (ip != null && ip.trim().length()>0) {
            executorAddress = ip.trim().concat(":").concat(String.valueOf(port));
        } else {
            executorAddress = IpUtil.getIpPort(port);
        }

        new ScheduledThreadPoolExecutor(1,
                MyTimingThreadFactory.create("MythAutoRecoverService",
                        true)).scheduleWithFixedDelay(() -> {
            logger.info("auto registry Thread begin");
            try {
                RegistryParam registryParam = new RegistryParam(RegistryConfig.RegistType.EXECUTOR.name(),appName,executorAddress);
                for(AdminBz adminBz : TimingJobExecutor.getAdminBizList()){
                    try {
                        ReturnT<String> registryResult = adminBz.registry(registryParam);
                        if(registryResult !=null && ReturnT.SUCCESS_CODE == registryResult.getCode()){
                            registryResult = ReturnT.SUCCESS;
                            logger.info(">>>>>>>>>>> timing job registry success, registryParam:{}, registryResult:{}", new Object[]{registryParam, registryResult});
                            break;
                        }else{
                            logger.info(">>>>>>>>>>> timing job registry failed, registryParam:{}, registryResult:{}", new Object[]{registryParam, registryResult});
                        }
                    } catch (Exception e) {
                        logger.info(">>>>>>>>>>> timing job registry error, registryParam:{}", registryParam, e);
                    }
                }


            } catch (Exception e) {
                e.printStackTrace();
            }
        }, 30, RegistryConfig.BEAT_TIMEOUT, TimeUnit.SECONDS);


    }


}
