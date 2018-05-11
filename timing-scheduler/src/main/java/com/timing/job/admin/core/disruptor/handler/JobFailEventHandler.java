package com.timing.job.admin.core.disruptor.handler;

import com.lmax.disruptor.EventHandler;
import com.timing.executor.core.biz.model.ReturnT;
import com.timing.job.admin.core.disruptor.event.JobFailEvent;
import com.timing.job.admin.core.disruptor.publisher.JobFailEventPublisher;
import com.timing.job.admin.core.model.TimingJobGroup;
import com.timing.job.admin.core.model.TimingJobInfo;
import com.timing.job.admin.core.model.TimingJobLog;
import com.timing.job.admin.core.schedule.TimingJobScheduler;
import com.timing.job.admin.core.thread.JobFailMonitorHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.MessageFormat;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * 失败事件处理器handler
 */
public class JobFailEventHandler implements EventHandler<JobFailEvent> {

    private static Logger logger = LoggerFactory.getLogger(JobFailEventHandler.class);

    private JobFailEventHandler(){}

    private static class JobFailEventHandlerHolder{
        static  final JobFailEventHandler handler = new JobFailEventHandler();
    }

    public static JobFailEventHandler getInstance(){
       return  JobFailEventHandlerHolder.handler;
    }

    @Override
    public void onEvent(JobFailEvent event, long sequence, boolean endOfBatch) throws Exception {
        System.out.println("--------------disruptor 处理失败事件开始----------------"+event.toString());
        try {
            logger.info("check fail job");
            Integer jobLogId = event.getJobId();
            if(jobLogId!=null && jobLogId>0){
                TimingJobLog log = TimingJobScheduler.timingJobLogDao.load(jobLogId);
                if(log!=null){
                    if (ReturnT.SUCCESS_CODE==log.getTriggerCode() && log.getHandleCode()==0) {
                        // running
                        try {
                            TimeUnit.SECONDS.sleep(10);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        //JobFailMonitorHelper.monitor(jobLogId);
                        JobFailEventPublisher.getInstance().publishEvent(jobLogId);
                    }
                    if (ReturnT.SUCCESS_CODE==log.getTriggerCode() && ReturnT.SUCCESS_CODE==log.getHandleCode()) {
                        // pass
                    }
                    if (ReturnT.FAIL_CODE == log.getTriggerCode()|| ReturnT.FAIL_CODE==log.getHandleCode()) {
                        TimingJobInfo info = TimingJobScheduler.timingJobInfoDao.loadById(log.getJobId());
                        if (info!=null && info.getAlarmEmail()!=null && info.getAlarmEmail().trim().length()>0) {

                            Set<String> emailSet = new HashSet<String>(Arrays.asList(info.getAlarmEmail().split(",")));
                            for (String email: emailSet) {
                                String title = "《调度监控报警》(任务调度中心 Timing-JOB)";
                                TimingJobGroup group = TimingJobScheduler.timingJobGroupDao.load(Integer.valueOf(info.getJobGroup()));
                                String content = MessageFormat.format("任务调度失败, 执行器名称:{0}, 任务描述:{1}.", group!=null?group.getTitle():"null", info.getJobDesc());
                                //MailUtil.sendMail(email, title, content, false, null);
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        System.out.println("--------------disruptor 处理失败事件结束----------------"+event.toString());
    }
}
