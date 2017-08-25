package com.timing.executor.core.biz.executor;

import com.timing.executor.core.biz.AdminBz;
import com.timing.executor.core.biz.ExecutorService;
import com.timing.executor.core.biz.handler.AbstractJobHandler;
import com.timing.executor.core.biz.handler.annotation.JobHandler;
import com.timing.executor.core.biz.impl.ExecutorServiceImpl;
import com.timing.executor.core.biz.log.TimingJobFileAppender;
import com.timing.executor.core.biz.rpc.net.jetty.NetClientProxy;
import com.timing.executor.core.biz.rpc.net.jetty.NetServerFactory;
import com.timing.executor.core.biz.thread.JobThread;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by winstone on 2017/8/21.
 */
public class TimingJobExecutor implements ApplicationContextAware {

    private static final Logger logger = LoggerFactory.getLogger(TimingJobExecutor.class);

    private String ip;
    private int port = 9999;
    private String appName;
    private String adminAddresses;
    private String accessToken;
    private String logPath;

    public void setIp(String ip) {
        this.ip = ip;
    }
    public void setPort(int port) {
        this.port = port;
    }
    public void setAppName(String appName) {
        this.appName = appName;
    }
    public void setAdminAddresses(String adminAddresses) {
        this.adminAddresses = adminAddresses;
    }
    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }
    public void setLogPath(String logPath) {
        this.logPath = logPath;
    }


    private static ApplicationContext applicationContext;

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
       this.applicationContext = applicationContext;
    }

    public static ApplicationContext getApplicationContext(){
        return  applicationContext;
    }


    public void start() throws Exception{

        initAdminBizList(adminAddresses, accessToken);//初始化注册中心

        initJobHandlerRepository(applicationContext);//初始化handler,执行器

        if(logPath !=null && logPath.trim().length()>0){ //初始化日志路径
            TimingJobFileAppender.logPath = logPath;
        }

        initExecutorServer(ip,port,appName,accessToken); //初始化执行器

    }


    public void destory(){
        if (jobThreadRepository.size() > 0) {
            for (Map.Entry<Integer, JobThread> item: jobThreadRepository.entrySet()) {
                removeJobThread(item.getKey(), "Web容器销毁终止");
            }
            jobThreadRepository.clear();
        }
        stopExecutorServer();
    }

    //初始化adminbiz列表
    private static List<AdminBz> adminBizList;
    private static void initAdminBizList(String adminAddresses, String accessToken) throws Exception {
        if (adminAddresses!=null && adminAddresses.trim().length()>0) {
            for (String address: adminAddresses.trim().split(",")) {
                if (address!=null && address.trim().length()>0) {
                    String addressUrl = address.concat(AdminBz.MAPPING);
                    AdminBz adminBiz = (AdminBz) new NetClientProxy(AdminBz.class, addressUrl, accessToken).getObject();
                    if (adminBizList == null) {
                        adminBizList = new ArrayList<AdminBz>();
                    }
                    adminBizList.add(adminBiz);
                }
            }
        }
    }
    public static List<AdminBz> getAdminBizList(){
        return adminBizList;
    }

    //初始化执行器handler
   private static ConcurrentHashMap<String,AbstractJobHandler> jobHandlerMap = new ConcurrentHashMap<>();

   public static AbstractJobHandler registHandlerMap(String name,AbstractJobHandler handler){
       return jobHandlerMap.put(name,handler);
   }

    public static AbstractJobHandler loadJobHandler(String name){
       return jobHandlerMap.get(name);
    }

    private static void initJobHandlerRepository(ApplicationContext applicationContext){
       Map<String,Object> serviceBeanMap = applicationContext.getBeansWithAnnotation(JobHandler.class);
       if(serviceBeanMap!=null && serviceBeanMap.values().size()>0){
          for(Object serviceBean : serviceBeanMap.values()){
             if(serviceBean instanceof  AbstractJobHandler){
                 String name = serviceBean.getClass().getAnnotation(JobHandler.class).value();
                 AbstractJobHandler handler = (AbstractJobHandler)serviceBean;
                 if(loadJobHandler(name)!=null){
                     throw new RuntimeException("job handler confilicts");
                 }
                 registHandlerMap(name,handler);
             }
          }
      }
    }


    //初始化执行器
    private NetServerFactory serverFactory = new NetServerFactory();
    private void initExecutorServer( String ip,int port, String appName, String accessToken) throws Exception {
        NetServerFactory.putService(ExecutorService.class, new ExecutorServiceImpl());
        NetServerFactory.setAccessToken(accessToken);
        serverFactory.start(ip,port,appName);
    }
    private void stopExecutorServer() {
        serverFactory.destroy();
    }


    //初始化线程repository
    private static  ConcurrentHashMap<Integer,JobThread> jobThreadRepository = new ConcurrentHashMap<>();

    //注册线程
    public static JobThread registJobThread(int jobId,AbstractJobHandler handler,String removeReason){
        JobThread newJobThread = new JobThread(jobId,handler);
        newJobThread.start();
        logger.info("regist job thread success,jobId:{},handler:{}",new Object[]{jobId,handler});

        JobThread oldThread = jobThreadRepository.put(jobId,newJobThread);
        if(oldThread != null){
            oldThread.toStop(removeReason);
            oldThread.interrupt();
        }
       return newJobThread;
    }

   //从线程map中删除线程
    public static void removeJobThread(int jobId,String removeReason){
        JobThread oldJobThread = jobThreadRepository.remove(jobId);
        if(null!=oldJobThread){
            oldJobThread.toStop(removeReason);
            oldJobThread.interrupt();
        }

    }

    //加载某个线程
    public static JobThread loadJobThread(int jobId){
        JobThread jobThread = jobThreadRepository.get(jobId);
        return jobThread;
    }


}
