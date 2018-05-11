package com.timing.job.admin.core.schedule;

import com.timing.executor.core.biz.AdminBz;
import com.timing.executor.core.biz.ExecutorService;
import com.timing.executor.core.biz.rpc.net.jetty.NetClientProxy;
import com.timing.executor.core.biz.rpc.net.jetty.NetServerFactory;
import com.timing.job.admin.core.disruptor.publisher.JobFailEventPublisher;
import com.timing.job.admin.core.jobbean.RemoteHttpJobBean;
import com.timing.job.admin.core.model.TimingJobInfo;
import com.timing.job.admin.core.thread.JobFailMonitorHelper;
import com.timing.job.admin.core.thread.JobRegistryMonitorHelper;
import com.timing.job.admin.dao.TimingJobGroupDao;
import com.timing.job.admin.dao.TimingJobInfoDao;
import com.timing.job.admin.dao.TimingJobLogDao;
import com.timing.job.admin.dao.TimingJobRegistryDao;
import org.quartz.*;
import org.quartz.impl.triggers.CronTriggerImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.util.Assert;

import java.util.Date;
import java.util.HashSet;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by winstone on 2017/8/23.
 */
public class TimingJobScheduler implements ApplicationContextAware{

    private static final Logger logger = LoggerFactory.getLogger(TimingJobScheduler.class);

    private static Scheduler scheduler;

    public void setScheduler(Scheduler scheduler){
        TimingJobScheduler.scheduler = scheduler;
    }

    private static String accessToken;
    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }


    //dao
    public static TimingJobLogDao timingJobLogDao;
    public static TimingJobInfoDao timingJobInfoDao;
    public static TimingJobRegistryDao timingJobRegistryDao;
    public static TimingJobGroupDao timingJobGroupDao;
    public static AdminBz adminBz;

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        TimingJobScheduler.timingJobLogDao = applicationContext.getBean(TimingJobLogDao.class);
        TimingJobScheduler.timingJobInfoDao = applicationContext.getBean(TimingJobInfoDao.class);
        TimingJobScheduler.timingJobRegistryDao = applicationContext.getBean(TimingJobRegistryDao.class);
        TimingJobScheduler.timingJobGroupDao = applicationContext.getBean(TimingJobGroupDao.class);
        TimingJobScheduler.adminBz = applicationContext.getBean(AdminBz.class);
    }


    //初始化，主要是初始化
    public void init()throws  Exception{

     //   JobRegistryMonitorHelper.getInstance().start();
        JobFailEventPublisher.getInstance().start(1024);
        // admin monitor run
        //JobFailMonitorHelper.getInstance().start();

        // admin-server(spring-mvc)
        NetServerFactory.putService(AdminBz.class, TimingJobScheduler.adminBz);
        NetServerFactory.setAccessToken(accessToken);

        // valid
        Assert.notNull(scheduler, "quartz scheduler is null");
        logger.info(">>>>>>>>> init quartz scheduler success.[{}]", scheduler);

    }


    public void destory(){
        // admin registry stop
        JobRegistryMonitorHelper.getInstance().toStop();

        // admin monitor stop
        //JobFailMonitorHelper.getInstance().toStop();
        JobFailEventPublisher.getInstance().destory();

    }


    //获取executorservice-client
    public static ConcurrentHashMap<String,ExecutorService> executorBizRepository = new ConcurrentHashMap<>();

    public static  ExecutorService getExecutorBiz(String address)throws Exception{

      if(address == null || address.trim().length()==0){
         return null;
      }
      address = address.trim();
      //load 数据
      ExecutorService executorService = executorBizRepository.get(address);
      if(executorService!=null){
          return executorService;
      }

     executorService = (ExecutorService)new NetClientProxy(ExecutorService.class,address,accessToken).getObject();
     executorBizRepository.put(address,executorService);
     return executorService;
    }


    //---------------------schedule job ------------------

    public static void fillJobInfo(TimingJobInfo jobInfo){
        // TriggerKey : name + group
        String group = String.valueOf(jobInfo.getJobGroup());
        String name = String.valueOf(jobInfo.getId());
        TriggerKey triggerKey = TriggerKey.triggerKey(name, group);

        try {
            Trigger trigger = scheduler.getTrigger(triggerKey);

            Trigger.TriggerState triggerState = scheduler.getTriggerState(triggerKey);

            // parse params
            if (trigger!=null && trigger instanceof CronTriggerImpl) {
                String cronExpression = ((CronTriggerImpl) trigger).getCronExpression();
                jobInfo.setJobCron(cronExpression);
            }

            if (triggerState!=null) {
                jobInfo.setJobStatus(triggerState.name());
            }

        } catch (SchedulerException e) {
            e.printStackTrace();
        }
    }

    //检查是否存在
    public static boolean checkExists(String jobName,String jobGroup)throws Exception{
       TriggerKey triggerKey = TriggerKey.triggerKey(jobName,jobGroup);
       return scheduler.checkExists(triggerKey);
    }


    //新增任务
    public static boolean addJob(String jobName,String jobGroup,String cronExpression) throws Exception{
        TriggerKey triggerKey = TriggerKey.triggerKey(jobName,jobGroup);
        JobKey jobKey = new JobKey(jobName,jobGroup);

        if(checkExists(jobName,jobGroup)){
            logger.info(">>>>>>>>> addJob fail, job already exist, jobGroup:{}, jobName:{}", jobGroup, jobName);
            return false;
        }

        // CronTrigger : TriggerKey + cronExpression	// withMisfireHandlingInstructionDoNothing 忽略掉调度终止过程中忽略的调度
        CronScheduleBuilder cronScheduleBuilder = CronScheduleBuilder.cronSchedule(cronExpression).withMisfireHandlingInstructionDoNothing();
        CronTrigger cronTrigger = TriggerBuilder.newTrigger().withIdentity(triggerKey).withSchedule(cronScheduleBuilder).build();

        // JobDetail : jobClass
        Class<? extends Job> jobClass_ = RemoteHttpJobBean.class;   // Class.forName(jobInfo.getJobClass());

        JobDetail jobDetail = JobBuilder.newJob(jobClass_).withIdentity(jobKey).build();
        /*if (jobInfo.getJobData()!=null) {
        	JobDataMap jobDataMap = jobDetail.getJobDataMap();
        	jobDataMap.putAll(JacksonUtil.readValue(jobInfo.getJobData(), Map.class));
        	// JobExecutionContext context.getMergedJobDataMap().get("mailGuid");
		}*/

        // schedule : jobDetail + cronTrigger
        Date date = scheduler.scheduleJob(jobDetail, cronTrigger);

        logger.info(">>>>>>>>>>> addJob success, jobDetail:{}, cronTrigger:{}, date:{}", jobDetail, cronTrigger, date);
        return true;
    }


    /**
     * rescheduleJob
     *
     * @param jobGroup
     * @param jobName
     * @param cronExpression
     * @return
     * @throws SchedulerException
     */
    public static boolean rescheduleJob(String jobGroup, String jobName, String cronExpression) throws Exception {

        // TriggerKey valid if_exists
        if (!checkExists(jobName, jobGroup)) {
            logger.info(">>>>>>>>>>> rescheduleJob fail, job not exists, JobGroup:{}, JobName:{}", jobGroup, jobName);
            return false;
        }

        // TriggerKey : name + group
        TriggerKey triggerKey = TriggerKey.triggerKey(jobName, jobGroup);
        CronTrigger oldTrigger = (CronTrigger) scheduler.getTrigger(triggerKey);

        if (oldTrigger != null) {
            // avoid repeat
            String oldCron = oldTrigger.getCronExpression();
            if (oldCron.equals(cronExpression)){
                return true;
            }

            // CronTrigger : TriggerKey + cronExpression
            CronScheduleBuilder cronScheduleBuilder = CronScheduleBuilder.cronSchedule(cronExpression).withMisfireHandlingInstructionDoNothing();
            oldTrigger = oldTrigger.getTriggerBuilder().withIdentity(triggerKey).withSchedule(cronScheduleBuilder).build();

            // rescheduleJob
            scheduler.rescheduleJob(triggerKey, oldTrigger);
        } else {
            // CronTrigger : TriggerKey + cronExpression
            CronScheduleBuilder cronScheduleBuilder = CronScheduleBuilder.cronSchedule(cronExpression).withMisfireHandlingInstructionDoNothing();
            CronTrigger cronTrigger = TriggerBuilder.newTrigger().withIdentity(triggerKey).withSchedule(cronScheduleBuilder).build();

            // JobDetail-JobDataMap fresh
            JobKey jobKey = new JobKey(jobName, jobGroup);
            JobDetail jobDetail = scheduler.getJobDetail(jobKey);
            /*JobDataMap jobDataMap = jobDetail.getJobDataMap();
            jobDataMap.clear();
            jobDataMap.putAll(JacksonUtil.readValue(jobInfo.getJobData(), Map.class));*/

            // Trigger fresh
            HashSet<Trigger> triggerSet = new HashSet<Trigger>();
            triggerSet.add(cronTrigger);

            scheduler.scheduleJob(jobDetail, triggerSet, true);
        }

        logger.info(">>>>>>>>>>> resumeJob success, JobGroup:{}, JobName:{}", jobGroup, jobName);
        return true;
    }



    /**
     * unscheduleJob
     *
     * @param jobName
     * @param jobGroup
     * @return
     * @throws SchedulerException
     */
    public static boolean removeJob(String jobName, String jobGroup) throws Exception {
        // TriggerKey : name + group
        TriggerKey triggerKey = TriggerKey.triggerKey(jobName, jobGroup);
        boolean result = false;
        if (checkExists(jobName, jobGroup)) {
            result = scheduler.unscheduleJob(triggerKey);
            logger.info(">>>>>>>>>>> removeJob, triggerKey:{}, result [{}]", triggerKey, result);
        }
        return true;
    }


    /**
     * pause
     *
     * @param jobName
     * @param jobGroup
     * @return
     * @throws SchedulerException
     */
    public static boolean pauseJob(String jobName, String jobGroup) throws Exception {
        // TriggerKey : name + group
        TriggerKey triggerKey = TriggerKey.triggerKey(jobName, jobGroup);
        boolean result = false;
        if (checkExists(jobName, jobGroup)) {
            scheduler.pauseTrigger(triggerKey);
            result = true;
            logger.info(">>>>>>>>>>> pauseJob success, triggerKey:{}", triggerKey);
        } else {
            logger.info(">>>>>>>>>>> pauseJob fail, triggerKey:{}", triggerKey);
        }
        return result;
    }



    /**
     * resume
     *
     * @param jobName
     * @param jobGroup
     * @return
     * @throws SchedulerException
     */
    public static boolean resumeJob(String jobName, String jobGroup) throws Exception {
        // TriggerKey : name + group
        TriggerKey triggerKey = TriggerKey.triggerKey(jobName, jobGroup);
        boolean result = false;
        if (checkExists(jobName, jobGroup)) {
            scheduler.resumeTrigger(triggerKey);
            result = true;
            logger.info(">>>>>>>>>>> resumeJob success, triggerKey:{}", triggerKey);
        } else {
            logger.info(">>>>>>>>>>> resumeJob fail, triggerKey:{}", triggerKey);
        }
        return result;
    }


    /**
     * run
     *
     * @param jobName
     * @param jobGroup
     * @return
     * @throws SchedulerException
     */
    public static boolean triggerJob(String jobName, String jobGroup) throws Exception {
        // TriggerKey : name + group
        JobKey jobKey = new JobKey(jobName, jobGroup);

        boolean result = false;
        if (checkExists(jobName, jobGroup)) {
            scheduler.triggerJob(jobKey);
            result = true;
            logger.info(">>>>>>>>>>> runJob success, jobKey:{}", jobKey);
        } else {
            logger.info(">>>>>>>>>>> runJob fail, jobKey:{}", jobKey);
        }
        return result;
    }



}
