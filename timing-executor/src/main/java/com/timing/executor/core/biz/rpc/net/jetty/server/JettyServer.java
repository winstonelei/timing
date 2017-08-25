package com.timing.executor.core.biz.rpc.net.jetty.server;

import com.timing.executor.core.biz.thread.ExecutorRegistryThread;
import com.timing.executor.core.biz.thread.TriggerCallbackThread;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.handler.HandlerCollection;
import org.eclipse.jetty.util.thread.ExecutorThreadPool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by winstone on 2017/8/21.
 */
public class JettyServer {

    private static final Logger logger = LoggerFactory.getLogger(JettyServer.class);

    private Server server;
    private Thread thread;
    public void start( final String ip,final int port, final String appName) throws Exception {
        thread = new Thread(new Runnable() {
            @Override
            public void run() {

                // The Server
                server = new Server(new ExecutorThreadPool());  // 非阻塞

                // HTTP connector
                ServerConnector connector = new ServerConnector(server);
                connector.setPort(port);
                server.setConnectors(new Connector[]{connector});

                // Set a handler
                HandlerCollection handlerc =new HandlerCollection();
                handlerc.setHandlers(new Handler[]{new JettyServerHandler()});
                server.setHandler(handlerc);

                try {
                    // Start server
                    server.start();
                    logger.info(">>>>>>>>>>>> timing job jetty server start success at port:{}.", port);

                    ExecutorRegistryThread.getInstance().start(ip,port,appName);

                    TriggerCallbackThread.getInstance().start();

                    server.join();	// block until thread stopped
                    logger.info(">>>>>>>>>>> timing rpc server join success, netcon={}, port={}", JettyServer.class.getName(), port);
                } catch (Exception e) {
                    logger.error("", e);
                } finally {
                    destroy();
                }
            }
        });
        thread.setDaemon(true);	// daemon, service jvm, user thread leave >>> daemon leave >>> jvm leave
        thread.start();
    }

    public void destroy() {

        // destroy server
        if (server != null) {
            try {
                server.stop();
                server.destroy();
            } catch (Exception e) {
                logger.error("", e);
            }
        }
        if (thread.isAlive()) {
            thread.interrupt();
        }

        // destroy Registry-Server
        ExecutorRegistryThread.getInstance().toStop();

        // destroy Callback-Server
        TriggerCallbackThread.getInstance().toStop();

        logger.info(">>>>>>>>>>> timing rpc server destroy success, netcon={}", JettyServer.class.getName());
    }

}
