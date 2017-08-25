package com.timing.executor.core.biz.rpc.net.jetty.client;

import com.timing.executor.core.biz.rpc.bean.RpcRequest;
import com.timing.executor.core.biz.rpc.bean.RpcResponse;
import com.timing.executor.core.biz.rpc.serilizable.HessianSerializer;
import com.timing.executor.core.biz.util.HttpClientUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by winstone on 2017/8/21.
 */
public class JettyClient {

    private static Logger logger = LoggerFactory.getLogger(JettyClient.class);


    public RpcResponse send(RpcRequest request) throws  Exception{
        try {
            byte[] requestBytes = HessianSerializer.serialize(request);

            String reqUrl = request.getServerAddress();

            if(reqUrl!=null && reqUrl.toLowerCase().indexOf("http://")==-1){
                reqUrl = "http://" + request.getServerAddress() + "/";
            }

            // remote invoke
            byte[] responseBytes = HttpClientUtil.postRequest(reqUrl, requestBytes);
            if (responseBytes == null || responseBytes.length==0) {
                RpcResponse rpcResponse = new RpcResponse();
                rpcResponse.setError("RpcResponse byte[] is null");
                return rpcResponse;
            }

            // deserialize response
            RpcResponse rpcResponse = (RpcResponse) HessianSerializer.deserialize(responseBytes, RpcResponse.class);
            return rpcResponse;
        } catch (Exception e) {
            e.printStackTrace();

            RpcResponse rpcResponse = new RpcResponse();
            rpcResponse.setError("Client-error:" + e.getMessage());
            return rpcResponse;
        }
    }



}
