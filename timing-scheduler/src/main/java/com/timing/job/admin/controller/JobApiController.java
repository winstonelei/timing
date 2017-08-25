package com.timing.job.admin.controller;

import com.timing.executor.core.biz.AdminBz;
import com.timing.executor.core.biz.rpc.bean.RpcRequest;
import com.timing.executor.core.biz.rpc.bean.RpcResponse;
import com.timing.executor.core.biz.rpc.net.jetty.NetServerFactory;
import com.timing.executor.core.biz.rpc.serilizable.HessianSerializer;
import com.timing.executor.core.biz.util.HttpClientUtil;
import com.timing.job.admin.controller.annoation.PermessionLimit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.OutputStream;

@Controller
public class JobApiController {
    private static Logger logger = LoggerFactory.getLogger(JobApiController.class);

    private RpcResponse doInvoke(HttpServletRequest request) {
        try {
            // deserialize request
            byte[] requestBytes = HttpClientUtil.readBytes(request);
            if (requestBytes == null || requestBytes.length==0) {
                RpcResponse rpcResponse = new RpcResponse();
                rpcResponse.setError("RpcRequest byte[] is null");
                return rpcResponse;
            }
            RpcRequest rpcRequest = (RpcRequest) HessianSerializer.deserialize(requestBytes, RpcRequest.class);

            // invoke
            RpcResponse rpcResponse = NetServerFactory.invokeService(rpcRequest, null);
            return rpcResponse;
        } catch (Exception e) {
            logger.error(e.getMessage(), e);

            RpcResponse rpcResponse = new RpcResponse();
            rpcResponse.setError("Server-error:" + e.getMessage());
            return rpcResponse;
        }
    }

    @RequestMapping(AdminBz.MAPPING)
    @PermessionLimit(limit=false)
    public void api(HttpServletRequest request, HttpServletResponse response) throws IOException {

        // invoke
        RpcResponse rpcResponse = doInvoke(request);

        // serialize response
        byte[] responseBytes = HessianSerializer.serialize(rpcResponse);

        response.setContentType("text/html;charset=utf-8");
        response.setStatus(HttpServletResponse.SC_OK);
        //baseRequest.setHandled(true);

        OutputStream out = response.getOutputStream();
        out.write(responseBytes);
        out.flush();
    }

}
