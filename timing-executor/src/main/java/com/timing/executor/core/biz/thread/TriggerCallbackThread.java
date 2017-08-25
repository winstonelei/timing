package com.timing.executor.core.biz.thread;

import com.timing.executor.core.biz.AdminBz;
import com.timing.executor.core.biz.executor.TimingJobExecutor;
import com.timing.executor.core.biz.model.HandleCallbackParam;
import com.timing.executor.core.biz.model.ReturnT;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Created by winstone on 2017/8/21.
 */
public class TriggerCallbackThread {

    private static Logger logger = LoggerFactory.getLogger(TriggerCallbackThread.class);

    private static TriggerCallbackThread instance = new TriggerCallbackThread();
    public static TriggerCallbackThread getInstance(){
        return instance;
    }
    private LinkedBlockingQueue<HandleCallbackParam> callBackQueue = new LinkedBlockingQueue<HandleCallbackParam>();

    private Thread triggerThread ;

    private boolean toStop = false ;

    public void start(){

       triggerThread = new Thread(new Runnable() {
           @Override
           public void run() {
             while(!toStop){
                 try {
                     HandleCallbackParam callbackParam = getInstance().callBackQueue.take();
                     if(callbackParam!=null){
                         // callback list param
                         List<HandleCallbackParam> callbackParamList = new ArrayList<HandleCallbackParam>();
                         int drainToNum = getInstance().callBackQueue.drainTo(callbackParamList);
                         callbackParamList.add(callbackParam);

                         if(TimingJobExecutor.getAdminBizList() == null){
                             logger.warn(">>>>>>>>>>>>  timing job callback fail, adminAddresses is null, callbackParamList：{}",callbackParamList);
                             continue;
                         }
                         for(AdminBz adminBz :TimingJobExecutor.getAdminBizList()){
                             try {
                                 ReturnT<String> callbackResult = adminBz.callback(callbackParamList);
                                 if(callbackResult!=null && ReturnT.SUCCESS_CODE == callbackResult.getCode()){
                                    callbackResult = ReturnT.SUCCESS;
                                    logger.info(">>>>>>>>>>> timing job callback success, callbackParamList:{}, callbackResult:{}", new Object[]{callbackParamList, callbackResult});
                                    break;
                                 }else{
                                     logger.info(">>>>>>>>>>> timing job callback fail, callbackParamList:{}, callbackResult:{}", new Object[]{callbackParamList, callbackResult});
                                 }
                             } catch (Exception e) {
                                 logger.error(">>>>>>>>>>> timing job callback error, callbackParamList：{}", callbackParamList, e);
                             }
                         }
                     }
                 } catch (Exception e) {
                     e.printStackTrace();
                     logger.error(e.getMessage());
                 }
             }
           }
       });

        //线程首先设置dameon
        triggerThread.setDaemon(true);
        triggerThread.start();


    }


    public static void pushCallBack(HandleCallbackParam callbackParam){
        try {
            getInstance().callBackQueue.add(callbackParam);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void toStop(){
        toStop = true;
    }



}
