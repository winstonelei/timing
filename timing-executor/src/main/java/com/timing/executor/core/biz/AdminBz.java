package com.timing.executor.core.biz;

import com.timing.executor.core.biz.model.HandleCallbackParam;
import com.timing.executor.core.biz.model.RegistryParam;
import com.timing.executor.core.biz.model.ReturnT;

import java.util.List;

/**
 * Created by winstone on 2017/8/18.
 */
public interface AdminBz {

    public static final String MAPPING = "/api";

    /**
     * callback
     *
     * @param callbackParamList
     * @return
     */
    public ReturnT<String> callback(List<HandleCallbackParam> callbackParamList);

    /**
     * registry
     *
     * @param registryParam
     * @return
     */
    public ReturnT<String> registry(RegistryParam registryParam);

}
