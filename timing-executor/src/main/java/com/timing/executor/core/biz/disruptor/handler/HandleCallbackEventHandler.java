package com.timing.executor.core.biz.disruptor.handler;

import com.lmax.disruptor.EventHandler;
import com.timing.executor.core.biz.AdminBz;
import com.timing.executor.core.biz.disruptor.event.HandleCallbackEvent;
import com.timing.executor.core.biz.disruptor.publisher.HandleCallbackPublisher;
import com.timing.executor.core.biz.executor.TimingJobExecutor;
import com.timing.executor.core.biz.model.HandleCallbackParam;
import com.timing.executor.core.biz.model.ReturnT;
import com.timing.executor.core.biz.rpc.net.jetty.server.JettyServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

public class HandleCallbackEventHandler  implements EventHandler<HandleCallbackEvent> {

    private static final Logger logger = LoggerFactory.getLogger(HandleCallbackEventHandler.class);

    private HandleCallbackEventHandler(){
    }

    private   static  class HandlerCallbackHolder{
        static final HandleCallbackEventHandler handler = new HandleCallbackEventHandler();
    }

    public static  HandleCallbackEventHandler getInstance(){
        return  HandlerCallbackHolder.handler;
    }

    @Override
    public void onEvent(HandleCallbackEvent event, long sequence, boolean endOfBatch) throws Exception {
        System.out.println("-------------disrupter 接收回调消息 begin---------------------");
        System.out.println(event.getHandleCallbackParam());
        List<HandleCallbackParam> callbackParamList = new ArrayList<>();
        callbackParamList.add(event.getHandleCallbackParam());
        for(AdminBz adminBz : TimingJobExecutor.getAdminBizList()){
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

        System.out.println("-------------disrupter 处理回调消息 end---------------------");
    }
}
