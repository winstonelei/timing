package com.timing.job.admin.core.thread;

import com.timing.executor.core.biz.enums.RegistryConfig;
import com.timing.job.admin.core.model.TimingJobGroup;
import com.timing.job.admin.core.model.TimingJobRegistry;
import com.timing.job.admin.core.schedule.TimingJobScheduler;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Created by winstone on 2017/8/23.
 */
public class JobRegistryMonitorHelper {

    private static Logger logger = LoggerFactory.getLogger(JobRegistryMonitorHelper.class);

    private static JobRegistryMonitorHelper instance = new JobRegistryMonitorHelper();
    public static JobRegistryMonitorHelper getInstance(){
        return instance;
    }

    private Thread registryThread;
    private boolean toStop = false;

    public void start(){
        registryThread = new Thread(new Runnable() {
            @Override
            public void run() {
              while (!toStop){
                 try{
                  List<TimingJobGroup> groupList = TimingJobScheduler.timingJobGroupDao.findByAddressType(0);
                  if (CollectionUtils.isNotEmpty(groupList)) {

                      // remove dead address (admin/executor)
                      TimingJobScheduler.timingJobRegistryDao.removeDead(RegistryConfig.DEAD_TIMEOUT);

                      // fresh online address (admin/executor)
                      HashMap<String, List<String>> appAddressMap = new HashMap<String, List<String>>();
                      List<TimingJobRegistry> list =  TimingJobScheduler.timingJobRegistryDao.findAll(RegistryConfig.DEAD_TIMEOUT);
                      if (list != null) {
                          for (TimingJobRegistry item: list) {
                              if (RegistryConfig.RegistType.EXECUTOR.name().equals(item.getRegistryGroup())) {
                                  String appName = item.getRegistryKey();
                                  List<String> registryList = appAddressMap.get(appName);
                                  if (registryList == null) {
                                      registryList = new ArrayList<String>();
                                  }

                                  if (!registryList.contains(item.getRegistryValue())) {
                                      registryList.add(item.getRegistryValue());
                                  }
                                  appAddressMap.put(appName, registryList);
                              }
                          }
                      }

                      // fresh group address
                      for (TimingJobGroup group: groupList) {
                          List<String> registryList = appAddressMap.get(group.getAppName());
                          String addressListStr = null;
                          if (CollectionUtils.isNotEmpty(registryList)) {
                              Collections.sort(registryList);
                              addressListStr = StringUtils.join(registryList, ",");
                          }
                          group.setAddressList(addressListStr);
                          TimingJobScheduler.timingJobGroupDao.update(group);
                      }
                  }
              } catch (Exception e) {
                    logger.error("job registry instance error:{}", e);
                }
                try {
                    TimeUnit.SECONDS.sleep(RegistryConfig.BEAT_TIMEOUT);
                } catch (InterruptedException e) {
                    logger.error("job registry instance error:{}", e);
                }

              }
            }
        });

        registryThread.setDaemon(true);
        registryThread.start();

    }


    public void toStop(){
        toStop = true;
    }




}
