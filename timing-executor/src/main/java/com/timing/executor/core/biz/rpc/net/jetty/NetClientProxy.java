package com.timing.executor.core.biz.rpc.net.jetty;

import com.timing.executor.core.biz.rpc.bean.RpcRequest;
import com.timing.executor.core.biz.rpc.bean.RpcResponse;
import com.timing.executor.core.biz.rpc.net.jetty.client.JettyClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.FactoryBean;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

/**
 * Created by winstone on 2017/8/21.
 */
public class NetClientProxy  implements FactoryBean<Object> {
    private static final Logger logger = LoggerFactory.getLogger(NetClientProxy.class);

    private Class<?> iface;
    private String serverAddress;
    private String accessToken;
    private JettyClient client = new JettyClient();

    public NetClientProxy(Class<?> iface, String serverAddress, String accessToken){
        this.iface = iface;
        this.serverAddress = serverAddress;
        this.accessToken = accessToken;
    }

    @Override
    public Object getObject() throws Exception {
        return Proxy.newProxyInstance(Thread.currentThread().getContextClassLoader(),new Class[]{iface},new InvocationHandler(){
            @Override
            public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                // request
                RpcRequest request = new RpcRequest();
                request.setServerAddress(serverAddress);
                request.setCreateMillisTime(System.currentTimeMillis());
                request.setAccessToken(accessToken);
                request.setClassName(method.getDeclaringClass().getName());
                request.setMethodName(method.getName());
                request.setParameterTypes(method.getParameterTypes());
                request.setParameters(args);

                // send
                RpcResponse response = client.send(request);

                // valid response
                if (response == null) {
                    logger.error(">>>>>>>>>>> timing -rpc netty response not found.");
                    throw new Exception(">>>>>>>>>>> timing -rpc netty response not found.");
                }
                if (response.isError()) {
                    throw new RuntimeException(response.getError());
                } else {
                    return response.getResult();
                }

            }
        });
    }

    @Override
    public Class<?> getObjectType() {
        return iface;
    }

    @Override
    public boolean isSingleton() {
        return false;
    }
}
