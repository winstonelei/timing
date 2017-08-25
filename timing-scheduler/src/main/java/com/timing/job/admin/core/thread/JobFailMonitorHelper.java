package com.timing.job.admin.core.thread;

import com.timing.executor.core.biz.model.ReturnT;
import com.timing.job.admin.core.model.TimingJobGroup;
import com.timing.job.admin.core.model.TimingJobInfo;
import com.timing.job.admin.core.model.TimingJobLog;
import com.timing.job.admin.core.schedule.TimingJobScheduler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.MessageFormat;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * Created by winstone on 2017/8/23.
 */
public class JobFailMonitorHelper {
    private static Logger logger = LoggerFactory.getLogger(JobFailMonitorHelper.class);

    private static JobFailMonitorHelper instance = new JobFailMonitorHelper();

    public static JobFailMonitorHelper getInstance(){
        return instance;
    }

   private LinkedBlockingQueue<Integer> queue = new LinkedBlockingQueue<>(0xfff8);

    private Thread monitorThread;

    private boolean toStop = false;

    public void start(){
        monitorThread = new Thread(new Runnable() {
            @Override
            public void run() {
                while(!toStop){
                    try {
                        logger.info("monitor beat");
                        Integer jobLogId = JobFailMonitorHelper.instance.queue.take();
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
                                        JobFailMonitorHelper.monitor(jobLogId);
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
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        });
        monitorThread.setDaemon(true);
        monitorThread.start();
    }


    public static void monitor(int jobId){
        getInstance().queue.offer(jobId);
    }

    public void toStop(){
        toStop = true;
    }


}
