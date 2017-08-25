package com.timing.executor.core.biz.rpc.net.jetty.server;

import com.timing.executor.core.biz.rpc.bean.RpcRequest;
import com.timing.executor.core.biz.rpc.bean.RpcResponse;
import com.timing.executor.core.biz.rpc.net.jetty.NetServerFactory;
import com.timing.executor.core.biz.rpc.serilizable.HessianSerializer;
import com.timing.executor.core.biz.util.HttpClientUtil;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.AbstractHandler;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.OutputStream;

/**
 * Created by winstone on 2017/8/21.
 */
public class JettyServerHandler  extends AbstractHandler {

    @Override
    public void handle(String target, Request request, HttpServletRequest httpServletRequest, HttpServletResponse response) throws IOException, ServletException {

        RpcResponse rpcResponse = doInvoke(httpServletRequest);

        byte[] reponseBytes = HessianSerializer.serialize(rpcResponse);

        response.setContentType("text/html;charset=utf-8");
        response.setStatus(HttpServletResponse.SC_OK);
        request.setHandled(true);

        OutputStream out = response.getOutputStream();
        out.write(reponseBytes);
        out.flush();
    }


    private RpcResponse doInvoke(HttpServletRequest request) {

        try {
            byte[] requestBytes = HttpClientUtil.readBytes(request);

            if (requestBytes == null || requestBytes.length == 0) {
                RpcResponse rpcResponse = new RpcResponse();
                rpcResponse.setError("RpcRequest byte[] is null");
                return rpcResponse;
            }
            RpcRequest rpcRequest = (RpcRequest) HessianSerializer.deserialize(requestBytes, RpcRequest.class);
            RpcResponse response = NetServerFactory.invokeService(rpcRequest, null);
            return response;

        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
     }
}
