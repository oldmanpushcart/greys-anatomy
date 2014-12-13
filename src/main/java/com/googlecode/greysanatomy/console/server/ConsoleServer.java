package com.googlecode.greysanatomy.console.server;

import com.googlecode.greysanatomy.Configure;
import com.googlecode.greysanatomy.console.rmi.RespResult;
import com.googlecode.greysanatomy.console.rmi.req.ReqCmd;
import com.googlecode.greysanatomy.console.rmi.req.ReqGetResult;
import com.googlecode.greysanatomy.console.rmi.req.ReqHeart;
import com.googlecode.greysanatomy.console.rmi.req.ReqKillJob;
import com.googlecode.greysanatomy.util.GaStringUtils;
import com.googlecode.greysanatomy.util.HostUtils;

import java.lang.instrument.Instrumentation;
import java.net.MalformedURLException;
import java.rmi.*;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * 控制台服务器
 *
 * @author vlinux
 */
public class ConsoleServer extends UnicastRemoteObject implements ConsoleServerService {

    private static final long serialVersionUID = 7625219488001802803L;

    private static final Logger logger = Logger.getLogger("greysanatomy");

    private final ConsoleServerHandler serverHandler;
    private final Configure configure;

    private Registry registry;
    private boolean bind = false;

    static {

        // fix java.rmi.server.hostname, if it was invalid
        final String javaRmiServerHostname = System.getProperty("java.rmi.server.hostname");
        if( GaStringUtils.isNotBlank(javaRmiServerHostname)
                && !HostUtils.getAllLocalHostIP().contains(javaRmiServerHostname)) {
            System.setProperty("java.rmi.server.hostname", "127.0.0.1");
        }

    }

    /**
     * 构造控制台服务器
     *
     * @param configure
     * @param inst
     * @throws RemoteException
     * @throws MalformedURLException
     */
    private ConsoleServer(Configure configure, final Instrumentation inst) throws RemoteException, MalformedURLException, AlreadyBoundException {
        super();
        serverHandler = new ConsoleServerHandler(this, inst);
        this.configure = configure;
        rebind();
    }

    /**
     * 绑定Naming
     *
     * @throws MalformedURLException
     * @throws RemoteException
     * @throws AlreadyBoundException
     */
    public synchronized void rebind() throws MalformedURLException, RemoteException, AlreadyBoundException {

        registry = LocateRegistry.createRegistry(configure.getTargetPort());
        for (String ip : HostUtils.getAllLocalHostIP()) {
            final String bindName = String.format("rmi://%s:%d/RMI_GREYS_ANATOMY", ip, configure.getTargetPort());
            try {
                logger.info("lookup for :"+bindName);
                Naming.lookup(bindName);
                bind = true;
            } catch (NotBoundException e) {
                // 只有没有绑定才会去绑
                if(logger.isLoggable(Level.INFO)) {
                    logger.info("rebind : " + bindName);
                }
                Naming.bind(bindName, this);
            }
        }

    }

    /**
     * 解除绑定Naming
     *
     * @throws RemoteException
     * @throws NotBoundException
     * @throws MalformedURLException
     */
    private synchronized void unbind() throws RemoteException, NotBoundException, MalformedURLException {

        for (String ip : HostUtils.getAllLocalHostIP()) {

            final String bindName = String.format("rmi://%s:%d/RMI_GREYS_ANATOMY", ip, configure.getTargetPort());
            try {
                Naming.unbind(bindName);
                if(logger.isLoggable(Level.INFO)){
                    logger.info("unbind : " + bindName);
                }
            } catch (NotBoundException e) {
                continue;
            } catch (NoSuchObjectException e) {
                continue;
            }
        }//for

        if (null != registry) {
            UnicastRemoteObject.unexportObject(registry, true);
//            PortableRemoteObject.unexportObject(this);
        }

        bind = false;

    }

    /**
     * 是否已被RMI.bind()
     *
     * @return
     */
    public boolean isBind() {
        return bind;
    }

    /**
     * 关闭ConsoleServer
     *
     * @throws RemoteException
     * @throws NotBoundException
     * @throws MalformedURLException
     */
    public void shutdown() throws RemoteException, NotBoundException, MalformedURLException {
        unbind();
    }


    private static volatile ConsoleServer instance;

    /**
     * 单例控制台服务器
     *
     * @param configure
     * @throws MalformedURLException
     * @throws RemoteException
     */
    public static synchronized ConsoleServer getInstance(Configure configure, Instrumentation inst) throws RemoteException, MalformedURLException, AlreadyBoundException {
        if (null == instance) {
            instance = new ConsoleServer(configure, inst);
            if(logger.isLoggable(Level.INFO)){
                logger.info(GaStringUtils.getLogo());
            }
        }
        return instance;
    }

    @Override
    public RespResult postCmd(ReqCmd cmd) throws Exception {
        return serverHandler.postCmd(cmd);
    }

    @Override
    public long register() throws Exception {
        return serverHandler.register();
    }

    @Override
    public boolean checkPID(int pid) throws Exception {
        return configure.getJavaPid() == pid;
    }

    @Override
    public RespResult getCmdExecuteResult(ReqGetResult req) throws Exception {
        return serverHandler.getCmdExecuteResult(req);
    }

    @Override
    public void killJob(ReqKillJob req) throws Exception {
        serverHandler.killJob(req);
    }

    @Override
    public boolean sessionHeartBeat(ReqHeart req) throws Exception {
        return serverHandler.sessionHeartBeat(req);
    }

    public Configure getConfigure() {
        return configure;
    }
}
