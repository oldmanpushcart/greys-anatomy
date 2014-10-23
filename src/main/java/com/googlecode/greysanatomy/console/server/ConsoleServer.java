package com.googlecode.greysanatomy.console.server;

import com.googlecode.greysanatomy.Configer;
import com.googlecode.greysanatomy.console.rmi.RespResult;
import com.googlecode.greysanatomy.console.rmi.req.ReqCmd;
import com.googlecode.greysanatomy.console.rmi.req.ReqGetResult;
import com.googlecode.greysanatomy.console.rmi.req.ReqHeart;
import com.googlecode.greysanatomy.console.rmi.req.ReqKillJob;
import com.googlecode.greysanatomy.util.GaStringUtils;
import com.googlecode.greysanatomy.util.HostUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.instrument.Instrumentation;
import java.net.MalformedURLException;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.server.UnicastRemoteObject;

/**
 * 控制台服务器
 *
 * @author vlinux
 */
public class ConsoleServer extends UnicastRemoteObject implements ConsoleServerService {

    private static final long serialVersionUID = 7625219488001802803L;

    private static final Logger logger = LoggerFactory.getLogger("greysanatomy");

    private final ConsoleServerHandler serverHandler;
    private final Configer configer;

    /**
     * 构造控制台服务器
     *
     * @param configer
     * @param inst
     * @throws RemoteException
     * @throws MalformedURLException
     */
    private ConsoleServer(Configer configer, final Instrumentation inst) throws RemoteException, MalformedURLException {
        super();
        serverHandler = new ConsoleServerHandler(this, inst);
        this.configer = configer;
        rebind();
    }

    /**
     * 绑定Naming
     * @throws MalformedURLException
     * @throws RemoteException
     */
    private void rebind() throws MalformedURLException, RemoteException {

        LocateRegistry.createRegistry(configer.getTargetPort());
        for( String ip : HostUtils.getAllLocalHostIP() ) {
            Naming.rebind(String.format("rmi://%s:%d/RMI_GREYS_ANATOMY",
                    ip,
                    configer.getTargetPort()),this);
        }

    }

    /**
     * 解除绑定Naming
     */
    private void unbind() throws RemoteException, NotBoundException, MalformedURLException {

        for( String ip : HostUtils.getAllLocalHostIP() ) {
            Naming.unbind(String.format("rmi://%s:%d/RMI_GREYS_ANATOMY",
                    ip,
                    configer.getTargetPort()));
        }

    }

    /**
     * 关闭ConsoleServer
     * @throws RemoteException
     * @throws NotBoundException
     * @throws MalformedURLException
     */
    public void shutdown() throws RemoteException, NotBoundException, MalformedURLException {
        unbind();
    }


    private static ConsoleServer instance;

    /**
     * 单例控制台服务器
     *
     * @param configer
     * @throws MalformedURLException
     * @throws RemoteException
     */
    public static synchronized ConsoleServer getInstance(Configer configer, Instrumentation inst) throws RemoteException, MalformedURLException {
        if (null == instance) {
            instance = new ConsoleServer(configer, inst);
            logger.info(GaStringUtils.getLogo());
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

}
