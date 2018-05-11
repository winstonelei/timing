package com.timing.executor.core.biz.thread;

import com.timing.executor.core.biz.disruptor.publisher.HandleCallbackPublisher;
import com.timing.executor.core.biz.executor.TimingJobExecutor;
import com.timing.executor.core.biz.handler.AbstractJobHandler;
import com.timing.executor.core.biz.log.TimingJobFileAppender;
import com.timing.executor.core.biz.log.TimingLogger;
import com.timing.executor.core.biz.model.HandleCallbackParam;
import com.timing.executor.core.biz.model.ReturnT;
import com.timing.executor.core.biz.model.TriggerParam;
import com.timing.executor.core.biz.util.ShardingUtil;
import org.eclipse.jetty.util.ConcurrentHashSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Arrays;
import java.util.Date;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * Created by winstone on 2017/8/22.
 */
public class JobThread extends Thread{

    private static Logger logger = LoggerFactory.getLogger(JobThread.class);

    private int jobId;
    private AbstractJobHandler handler;
    private LinkedBlockingQueue<TriggerParam> triggerQueue;
    private ConcurrentHashSet<Integer> triggerLogIdSet;


    private boolean toStop = false;
    private String stopReason;

    private boolean running = false;
    private int idleTimes = 0;

    public JobThread(int jobId,AbstractJobHandler handler){
        this.jobId = jobId;
        this.handler = handler;
        this.triggerQueue = new LinkedBlockingQueue<>();
        this.triggerLogIdSet = new ConcurrentHashSet<>();
    }


   public AbstractJobHandler getHandler(){return handler;}


   public ReturnT<String> pushTriggerQueue(TriggerParam triggerParam){
      if(triggerLogIdSet.contains(triggerParam.getJobId())){
          logger.debug("repeate trigger job, logId:{}", triggerParam.getLogId());
          return new ReturnT<String>(ReturnT.FAIL_CODE, "repeate trigger job, logId:" + triggerParam.getLogId());
      }
     triggerLogIdSet.add(triggerParam.getJobId());
     triggerQueue.add(triggerParam);
     return   ReturnT.SUCCESS;
   }


    /**
     * kill job thread
      * @param stopReason
     */
  public void toStop(String stopReason){
      /**
       * Thread.interrupt只支持终止线程的阻塞状态(wait、join、sleep)，
       * 在阻塞出抛出InterruptedException异常,但是并不会终止运行的线程本身；
       * 所以需要注意，此处彻底销毁本线程，需要通过共享变量方式；
       */
      this.toStop = true;
      this.stopReason = stopReason;
  }

    /**
     * 判断线程是否运行或者是否有正在运行的job
      * @return
     */
  public boolean isRunningOrHasQueue(){
      return running || triggerQueue.size()>0;
  }


   public void run(){
      while(!toStop){
          running = false;
          idleTimes ++;
          try {
              TriggerParam triggerParam = (TriggerParam) triggerQueue.poll(3L, TimeUnit.MILLISECONDS);
              if(triggerParam!= null){
                running = true;
                idleTimes = 0;
                triggerLogIdSet.remove(triggerParam.getJobId());

                String[]  handleParams =  (triggerParam.getExecutorParams()!=null && triggerParam.getExecutorParams().trim().length()>0)
                        ? (String[])(Arrays.asList(triggerParam.getExecutorParams().split(",")).toArray()) : null;

                ReturnT<String> executeResult = null;

                  try {
                      String logFileName = TimingJobFileAppender.makeLogFileName(new Date(triggerParam.getLogDateTim()),triggerParam.getJobId());

                      TimingJobFileAppender.contextHolder.set(logFileName);
                      ShardingUtil.setShardingVo(new ShardingUtil.ShardingVO(triggerParam.getBroadcastIndex(), triggerParam.getBroadcastTotal()));

                      TimingLogger.log("<br>----------- timing-job job execute start -----------<br>----------- Params:" + Arrays.toString(handleParams));


                      executeResult = handler.execute(handleParams);

                      if(null == executeResult){
                          executeResult = ReturnT.FAIL;
                      }

                      TimingLogger.log("<br>----------- timing-job job execute end(finish) -----------<br>----------- ReturnT:" + executeResult);

                  } catch (Exception e) {
                      if (toStop) {
                          TimingLogger.log("<br>----------- JobThread toStop, stopReason:" + stopReason);
                      }

                      StringWriter stringWriter = new StringWriter();
                      e.printStackTrace(new PrintWriter(stringWriter));
                      String errorMsg = stringWriter.toString();
                      executeResult = new ReturnT<String>(ReturnT.FAIL_CODE, errorMsg);

                      TimingLogger.log("<br>----------- JobThread Exception:" + errorMsg + "<br>----------- timing-job job execute end(error) -----------");
                  }


                  if(!toStop){
                    //  TriggerCallbackThread.pushCallBack(new HandleCallbackParam(triggerParam.getLogId(), executeResult));
                      HandleCallbackPublisher.getInstance().publishEvent(new HandleCallbackParam(triggerParam.getLogId(), executeResult));
                  }else{
                      ReturnT<String> stopResult = new ReturnT<String>(ReturnT.FAIL_CODE, stopReason + " [业务运行中，被强制终止]");
                  //    TriggerCallbackThread.pushCallBack(new HandleCallbackParam(triggerParam.getLogId(), stopResult));
                      HandleCallbackPublisher.getInstance().publishEvent(new HandleCallbackParam(triggerParam.getLogId(), stopResult));
                  }

              }else{
                  if(idleTimes > 30){
                      TimingJobExecutor.removeJobThread(jobId,"executor idel times over limit");
                  }

              }
          } catch (Throwable e) {
              if (toStop) {
                  TimingLogger.log("<br>----------- timing job toStop, stopReason:" + stopReason);
              }

              StringWriter stringWriter = new StringWriter();
              e.printStackTrace(new PrintWriter(stringWriter));
              String errorMsg = stringWriter.toString();
              TimingLogger.log("----------- timing-job JobThread Exception:" + errorMsg);
          }

      }

       //判断回调队列中是否有值，如果值不为空，那么可以从队列中取值并且执行
       while (triggerQueue !=null && triggerQueue.size()>0){
          TriggerParam triggerParam = triggerQueue.poll();
          if(triggerParam!=null){
              ReturnT<String> stopResult = new ReturnT<String>(ReturnT.FAIL_CODE, stopReason + " [任务尚未执行，在调度队列中被终止]");
           //   TriggerCallbackThread.pushCallBack(new HandleCallbackParam(triggerParam.getLogId(), stopResult));
              HandleCallbackPublisher.getInstance().publishEvent(new HandleCallbackParam(triggerParam.getLogId(), stopResult));
          }
       }

       logger.info(">>>>>>>>>>>> Timing JobThread stoped, hashCode:{}", Thread.currentThread());
   }


}
