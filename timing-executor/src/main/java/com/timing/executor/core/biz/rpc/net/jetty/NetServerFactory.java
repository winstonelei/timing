package com.timing.executor.core.biz.rpc.net.jetty;

import com.timing.executor.core.biz.model.ReturnT;
import com.timing.executor.core.biz.rpc.bean.RpcRequest;
import com.timing.executor.core.biz.rpc.bean.RpcResponse;
import com.timing.executor.core.biz.rpc.net.jetty.server.JettyServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cglib.reflect.FastClass;
import org.springframework.cglib.reflect.FastMethod;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by winstone on 2017/8/21.
 */
public class NetServerFactory {

    private static final Logger logger = LoggerFactory.getLogger(NetServerFactory.class);

    JettyServer server = new JettyServer();

    public void start(String ip,int port,String appName)throws Exception{
        server.start( ip, port, appName);
    }

    public void destroy(){
        server.destroy();
    }


    private static Map<String,Object> seriveMap = new HashMap<>();
    private static String accessToken;
    public static void putService(Class<?> iface, Object serviceBean){
        seriveMap.put(iface.getName(), serviceBean);
    }
    public static void setAccessToken(String accessToken) {
        NetServerFactory.accessToken = accessToken;
    }

    public static RpcResponse invokeService(RpcRequest request, Object serviceBean){
       if(null == serviceBean){
           serviceBean = seriveMap.get(request.getClassName());
       }

        RpcResponse response = new RpcResponse();

        if (System.currentTimeMillis() - request.getCreateMillisTime() > 180000) {
            response.setResult(new ReturnT<String>(ReturnT.FAIL_CODE, "The timestamp difference between admin and executor exceeds the limit."));
            return response;
        }

        if (accessToken!=null && accessToken.trim().length()>0 && !accessToken.trim().equals(request.getAccessToken())) {
            response.setResult(new ReturnT<String>(ReturnT.FAIL_CODE, "The access token[" + request.getAccessToken() + "] is wrong."));
            return response;
        }

        try {
            Class<?> serviceClass = serviceBean.getClass();
            String methodName = request.getMethodName();
            Class<?>[] parameterTypes = request.getParameterTypes();
            Object[] parameters = request.getParameters();

            FastClass serviceFastClass = FastClass.create(serviceClass);
            FastMethod serviceFastMethod = serviceFastClass.getMethod(methodName, parameterTypes);

            Object result = serviceFastMethod.invoke(serviceBean, parameters);

            response.setResult(result);
        } catch (Throwable t) {
            t.printStackTrace();
            response.setError(t.getMessage());
        }

        return response;

    }

}
