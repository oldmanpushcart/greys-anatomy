package com.googlecode.greysanatomy.console.rmi.req;


/**
 * 请求命令
 *
 * @author chengtongda
 */
public class ReqCmd extends GaRequest {
    private static final long serialVersionUID = 7156731632312708537L;

    /**
     * 请求命令原始字符串
     */
    private String command;

    public ReqCmd(String commond, long sessionId) {
        this.command = commond;
        setGaSessionId(sessionId);
    }

    /**
     * 获取请求命令原始字符串
     *
     * @return
     */
    public String getCommand() {
        return command;
    }

    /**
     * 这是请求命令原始字符串
     *
     * @param command
     */
    public void setCommand(String command) {
        this.command = command;
    }

}
