package com.timing.job.admin.service.impl;

import com.timing.executor.core.biz.groovy.GlueTypeEnum;
import com.timing.executor.core.biz.model.ExecutorBlockStrategyEnum;
import com.timing.executor.core.biz.model.ReturnT;
import com.timing.job.admin.core.enums.ExecutorFailStrategyEnum;
import com.timing.job.admin.core.model.TimingJobGroup;
import com.timing.job.admin.core.model.TimingJobInfo;
import com.timing.job.admin.core.route.ExecutorRouteStrategyEnum;
import com.timing.job.admin.core.schedule.TimingJobScheduler;
import com.timing.job.admin.dao.TimingJobGroupDao;
import com.timing.job.admin.dao.TimingJobInfoDao;
import com.timing.job.admin.dao.TimingJobLogDao;
import com.timing.job.admin.dao.TimingJobLogGlueDao;
import com.timing.job.admin.service.TimingJobService;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.time.DateUtils;
import org.apache.commons.lang.time.FastDateFormat;
import org.quartz.CronExpression;
import org.quartz.SchedulerException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.text.MessageFormat;
import java.util.*;

/**
 * Created by winstone on 2017/8/23.
 */

@Service
public class TimingJobServiceImpl implements TimingJobService {

    private static Logger logger = LoggerFactory.getLogger(TimingJobServiceImpl.class);

    @Resource
    private TimingJobGroupDao timingJobGroupDao;
    @Resource
    private TimingJobInfoDao timingJobInfoDao;
    @Resource
    public TimingJobLogDao timingJobLogDao;
    @Resource
    private TimingJobLogGlueDao timingJobLogGlueDao;

    @Override
    public Map<String, Object> pageList(int start, int length, int jobGroup, String executorHandler, String filterTime) {
        // page list
        List<TimingJobInfo> list = timingJobInfoDao.pageList(start, length, jobGroup, executorHandler);
        int list_count = timingJobInfoDao.pageListCount(start, length, jobGroup, executorHandler);

        // fill job info
        if (list!=null && list.size()>0) {
            for (TimingJobInfo jobInfo : list) {
                TimingJobScheduler.fillJobInfo(jobInfo);
            }
        }

        // package result
        Map<String, Object> maps = new HashMap<String, Object>();
        maps.put("recordsTotal", list_count);		// 总记录数
        maps.put("recordsFiltered", list_count);	// 过滤后的总记录数
        maps.put("data", list);  					// 分页列表
        return maps;
    }

    @Override
    public ReturnT<String> add(TimingJobInfo jobInfo) {
        // valid
        TimingJobGroup group = timingJobGroupDao.load(jobInfo.getJobGroup());
        if (group == null) {
            return new ReturnT<String>(ReturnT.FAIL_CODE, "请选择“执行器”");
        }
        if (!CronExpression.isValidExpression(jobInfo.getJobCron())) {
            return new ReturnT<String>(ReturnT.FAIL_CODE, "请输入格式正确的“Cron”");
        }
        if (StringUtils.isBlank(jobInfo.getJobDesc())) {
            return new ReturnT<String>(ReturnT.FAIL_CODE, "请输入“任务描述”");
        }
        if (StringUtils.isBlank(jobInfo.getAuthor())) {
            return new ReturnT<String>(ReturnT.FAIL_CODE, "请输入“负责人”");
        }
        if (ExecutorRouteStrategyEnum.match(jobInfo.getExecutorRouteStrategy(), null) == null) {
            return new ReturnT<String>(ReturnT.FAIL_CODE, "路由策略非法");
        }
        if (ExecutorBlockStrategyEnum.match(jobInfo.getExecutorBlockStrategy(), null) == null) {
            return new ReturnT<String>(ReturnT.FAIL_CODE, "阻塞处理策略非法");
        }
        if (ExecutorFailStrategyEnum.match(jobInfo.getExecutorFailStrategy(), null) == null) {
            return new ReturnT<String>(ReturnT.FAIL_CODE, "失败处理策略非法");
        }
        if (GlueTypeEnum.match(jobInfo.getGlueType()) == null) {
            return new ReturnT<String>(ReturnT.FAIL_CODE, "运行模式非法非法");
        }
        if (GlueTypeEnum.BEAN==GlueTypeEnum.match(jobInfo.getGlueType()) && StringUtils.isBlank(jobInfo.getExecutorHandler())) {
            return new ReturnT<String>(ReturnT.FAIL_CODE, "请输入“JobHandler”");
        }

        // fix "\r" in shell
        if (GlueTypeEnum.GLUE_SHELL==GlueTypeEnum.match(jobInfo.getGlueType()) && jobInfo.getGlueSource()!=null) {
            jobInfo.setGlueSource(jobInfo.getGlueSource().replaceAll("\r", ""));
        }

        // childJobKey valid
        if (StringUtils.isNotBlank(jobInfo.getChildJobKey())) {
            String[] childJobKeys = jobInfo.getChildJobKey().split(",");
            for (String childJobKeyItem: childJobKeys) {
                String[] childJobKeyArr = childJobKeyItem.split("_");
                if (childJobKeyArr.length!=2) {
                    return new ReturnT<String>(ReturnT.FAIL_CODE, MessageFormat.format("子任务Key({0})格式错误", childJobKeyItem));
                }
                TimingJobInfo childJobInfo = timingJobInfoDao.loadById(Integer.valueOf(childJobKeyArr[1]));
                if (childJobInfo==null) {
                    return new ReturnT<String>(ReturnT.FAIL_CODE, MessageFormat.format("子任务Key({0})无效", childJobKeyItem));
                }
            }
        }

        // add in db
        timingJobInfoDao.save(jobInfo);
        if (jobInfo.getId() < 1) {
            return new ReturnT<String>(ReturnT.FAIL_CODE, "新增任务失败");
        }

        // add in quartz
        String qz_group = String.valueOf(jobInfo.getJobGroup());
        String qz_name = String.valueOf(jobInfo.getId());
        try {
            TimingJobScheduler.addJob(qz_name, qz_group, jobInfo.getJobCron());
            //XxlJobDynamicScheduler.pauseJob(qz_name, qz_group);
            return ReturnT.SUCCESS;
        } catch (Exception e) {
            logger.error("", e);
            try {
                timingJobInfoDao.delete(jobInfo.getId());
                TimingJobScheduler.removeJob(qz_name, qz_group);
            } catch (Exception e1) {
                logger.error("", e1);
            }
            return new ReturnT<String>(ReturnT.FAIL_CODE, "新增任务失败:" + e.getMessage());
        }
    }

    @Override
    public ReturnT<String> reschedule(TimingJobInfo jobInfo) {
        // valid
        if (!CronExpression.isValidExpression(jobInfo.getJobCron())) {
            return new ReturnT<String>(ReturnT.FAIL_CODE, "请输入格式正确的“Cron”");
        }
        if (StringUtils.isBlank(jobInfo.getJobDesc())) {
            return new ReturnT<String>(ReturnT.FAIL_CODE, "请输入“任务描述”");
        }
        if (StringUtils.isBlank(jobInfo.getAuthor())) {
            return new ReturnT<String>(ReturnT.FAIL_CODE, "请输入“负责人”");
        }
        if (ExecutorRouteStrategyEnum.match(jobInfo.getExecutorRouteStrategy(), null) == null) {
            return new ReturnT<String>(ReturnT.FAIL_CODE, "路由策略非法");
        }
        if (ExecutorBlockStrategyEnum.match(jobInfo.getExecutorBlockStrategy(), null) == null) {
            return new ReturnT<String>(ReturnT.FAIL_CODE, "阻塞处理策略非法");
        }
        if (ExecutorFailStrategyEnum.match(jobInfo.getExecutorFailStrategy(), null) == null) {
            return new ReturnT<String>(ReturnT.FAIL_CODE, "失败处理策略非法");
        }

        // childJobKey valid
        if (StringUtils.isNotBlank(jobInfo.getChildJobKey())) {
            String[] childJobKeys = jobInfo.getChildJobKey().split(",");
            for (String childJobKeyItem: childJobKeys) {
                String[] childJobKeyArr = childJobKeyItem.split("_");
                if (childJobKeyArr.length!=2) {
                    return new ReturnT<String>(ReturnT.FAIL_CODE, MessageFormat.format("子任务Key({0})格式错误", childJobKeyItem));
                }
                TimingJobInfo childJobInfo = timingJobInfoDao.loadById(Integer.valueOf(childJobKeyArr[1]));
                if (childJobInfo==null) {
                    return new ReturnT<String>(ReturnT.FAIL_CODE, MessageFormat.format("子任务Key({0})无效", childJobKeyItem));
                }
            }
        }

        // stage job info
        TimingJobInfo exists_jobInfo = timingJobInfoDao.loadById(jobInfo.getId());
        if (exists_jobInfo == null) {
            return new ReturnT<String>(ReturnT.FAIL_CODE, "参数异常");
        }
        //String old_cron = exists_jobInfo.getJobCron();

        exists_jobInfo.setJobCron(jobInfo.getJobCron());
        exists_jobInfo.setJobDesc(jobInfo.getJobDesc());
        exists_jobInfo.setAuthor(jobInfo.getAuthor());
        exists_jobInfo.setAlarmEmail(jobInfo.getAlarmEmail());
        exists_jobInfo.setExecutorRouteStrategy(jobInfo.getExecutorRouteStrategy());
        exists_jobInfo.setExecutorHandler(jobInfo.getExecutorHandler());
        exists_jobInfo.setExecutorParam(jobInfo.getExecutorParam());
        exists_jobInfo.setExecutorBlockStrategy(jobInfo.getExecutorBlockStrategy());
        exists_jobInfo.setExecutorFailStrategy(jobInfo.getExecutorFailStrategy());
        exists_jobInfo.setChildJobKey(jobInfo.getChildJobKey());
        timingJobInfoDao.update(exists_jobInfo);

        // fresh quartz
        String qz_group = String.valueOf(exists_jobInfo.getJobGroup());
        String qz_name = String.valueOf(exists_jobInfo.getId());
        try {
            boolean ret = TimingJobScheduler.rescheduleJob(qz_group, qz_name, exists_jobInfo.getJobCron());
            return ret?ReturnT.SUCCESS:ReturnT.FAIL;
        } catch (Exception e) {
            logger.error("", e);
        }

        return ReturnT.FAIL;
    }

    @Override
    public ReturnT<String> remove(int id) {
        TimingJobInfo xxlJobInfo = timingJobInfoDao.loadById(id);
        String group = String.valueOf(xxlJobInfo.getJobGroup());
        String name = String.valueOf(xxlJobInfo.getId());
        try {
            TimingJobScheduler.removeJob(name, group);
            timingJobInfoDao.delete(id);
            timingJobLogDao.delete(id);
            timingJobLogGlueDao.deleteByJobId(id);
            return ReturnT.SUCCESS;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return ReturnT.FAIL;
    }

    @Override
    public ReturnT<String> pause(int id) {
        TimingJobInfo xxlJobInfo = timingJobInfoDao.loadById(id);
        String group = String.valueOf(xxlJobInfo.getJobGroup());
        String name = String.valueOf(xxlJobInfo.getId());

        try {
            boolean ret = TimingJobScheduler.pauseJob(name, group);	// jobStatus do not store
            return ret?ReturnT.SUCCESS:ReturnT.FAIL;
        } catch (Exception e) {
            e.printStackTrace();
            return ReturnT.FAIL;
        }
    }

    @Override
    public ReturnT<String> resume(int id) {
        TimingJobInfo xxlJobInfo = timingJobInfoDao.loadById(id);
        String group = String.valueOf(xxlJobInfo.getJobGroup());
        String name = String.valueOf(xxlJobInfo.getId());

        try {
            boolean ret = TimingJobScheduler.resumeJob(name, group);
            return ret?ReturnT.SUCCESS:ReturnT.FAIL;
        } catch (Exception e) {
            e.printStackTrace();
            return ReturnT.FAIL;
        }
    }

    @Override
    public ReturnT<String> triggerJob(int id) {
        TimingJobInfo xxlJobInfo = timingJobInfoDao.loadById(id);
        String group = String.valueOf(xxlJobInfo.getJobGroup());
        String name = String.valueOf(xxlJobInfo.getId());

        try {
            TimingJobScheduler.triggerJob(name, group);
            return ReturnT.SUCCESS;
        } catch (Exception e) {
            e.printStackTrace();
            return ReturnT.FAIL;
        }
    }

    @Override
    public Map<String, Object> dashboardInfo() {
        int jobInfoCount = timingJobInfoDao.findAllCount();
        int jobLogCount = timingJobLogDao.triggerCountByHandleCode(-1);
        int jobLogSuccessCount = timingJobLogDao.triggerCountByHandleCode(ReturnT.SUCCESS_CODE);

        // executor count
        Set<String> executerAddressSet = new HashSet<String>();
        List<TimingJobGroup> groupList = timingJobGroupDao.findAll();

        if (CollectionUtils.isNotEmpty(groupList)) {
            for (TimingJobGroup group: groupList) {
                if (CollectionUtils.isNotEmpty(group.getRegistryList())) {
                    executerAddressSet.addAll(group.getRegistryList());
                }
            }
        }

        int executorCount = executerAddressSet.size();

        Map<String, Object> dashboardMap = new HashMap<String, Object>();
        dashboardMap.put("jobInfoCount", jobInfoCount);
        dashboardMap.put("jobLogCount", jobLogCount);
        dashboardMap.put("jobLogSuccessCount", jobLogSuccessCount);
        dashboardMap.put("executorCount", executorCount);
        return dashboardMap;
    }

    @Override
    public ReturnT<Map<String, Object>> triggerChartDate() {
        Date from = DateUtils.addDays(new Date(), -30);
        Date to = new Date();

        List<String> triggerDayList = new ArrayList<String>();
        List<Integer> triggerDayCountSucList = new ArrayList<Integer>();
        List<Integer> triggerDayCountFailList = new ArrayList<Integer>();
        int triggerCountSucTotal = 0;
        int triggerCountFailTotal = 0;

        List<Map<String, Object>> triggerCountMapAll = timingJobLogDao.triggerCountByDay(from, to, -1);
        List<Map<String, Object>> triggerCountMapSuc = timingJobLogDao.triggerCountByDay(from, to, ReturnT.SUCCESS_CODE);
        if (CollectionUtils.isNotEmpty(triggerCountMapAll)) {
            for (Map<String, Object> item: triggerCountMapAll) {
                String day = String.valueOf(item.get("triggerDay"));
                int dayAllCount = Integer.valueOf(String.valueOf(item.get("triggerCount")));
                int daySucCount = 0;
                int dayFailCount = dayAllCount - daySucCount;

                if (CollectionUtils.isNotEmpty(triggerCountMapSuc)) {
                    for (Map<String, Object> sucItem: triggerCountMapSuc) {
                        String daySuc = String.valueOf(sucItem.get("triggerDay"));
                        if (day.equals(daySuc)) {
                            daySucCount = Integer.valueOf(String.valueOf(sucItem.get("triggerCount")));
                            dayFailCount = dayAllCount - daySucCount;
                        }
                    }
                }

                triggerDayList.add(day);
                triggerDayCountSucList.add(daySucCount);
                triggerDayCountFailList.add(dayFailCount);
                triggerCountSucTotal += daySucCount;
                triggerCountFailTotal += dayFailCount;
            }
        } else {
            for (int i = 4; i > -1; i--) {
                triggerDayList.add(FastDateFormat.getInstance("yyyy-MM-dd").format(DateUtils.addDays(new Date(), -i)));
                triggerDayCountSucList.add(0);
                triggerDayCountFailList.add(0);
            }
        }

        Map<String, Object> result = new HashMap<String, Object>();
        result.put("triggerDayList", triggerDayList);
        result.put("triggerDayCountSucList", triggerDayCountSucList);
        result.put("triggerDayCountFailList", triggerDayCountFailList);
        result.put("triggerCountSucTotal", triggerCountSucTotal);
        result.put("triggerCountFailTotal", triggerCountFailTotal);
        return new ReturnT<Map<String, Object>>(result);
    }
}



