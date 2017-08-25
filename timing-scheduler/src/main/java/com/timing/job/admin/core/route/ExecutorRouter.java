package com.timing.job.admin.core.route;

import com.timing.executor.core.biz.model.ReturnT;
import com.timing.executor.core.biz.model.TriggerParam;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;

/**
 * Created by winstone on 2017/8/23.
 */
public abstract  class ExecutorRouter {

    protected static Logger logger = LoggerFactory.getLogger(ExecutorRouter.class);


    public abstract ReturnT<String> routeRun(TriggerParam triggerParam, ArrayList<String> addressList);


}
