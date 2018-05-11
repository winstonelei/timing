package com.timing.executor.core.biz.disruptor.translator;

import com.lmax.disruptor.EventTranslatorOneArg;
import com.timing.executor.core.biz.disruptor.event.HandleCallbackEvent;
import com.timing.executor.core.biz.model.HandleCallbackParam;

/**
 * 〈一句话功能简述〉<br>
 * 〈功能详细描述〉
 *
 * @author 17090718
 * @see [相关类/方法]（可选）
 * @since [产品/模块版本] （可选）
 */
public class HandleCallbackEventTranslator implements EventTranslatorOneArg<HandleCallbackEvent, HandleCallbackParam> {

    @Override
    public void translateTo(HandleCallbackEvent event, long sequence, HandleCallbackParam callbackParam) {
        event.setHandleCallbackParam(callbackParam);
    }
}
