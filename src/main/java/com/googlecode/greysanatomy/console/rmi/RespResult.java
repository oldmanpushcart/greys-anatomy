package com.googlecode.greysanatomy.console.rmi;

import java.io.Serializable;

/**
 * 服务端响应结果
 *
 * @author chengtongda
 */
public class RespResult implements Serializable {
    private static final long serialVersionUID = 661800158888334705L;

    private String jobId;

    private long sessionId;

    private int pos;

    private String message;

    private boolean isFinish;

    public String getJobId() {
        return jobId;
    }

    public void setJobId(String jobId) {
        this.jobId = jobId;
    }

    public long getSessionId() {
        return sessionId;
    }

    public void setSessionId(long sessionId) {
        this.sessionId = sessionId;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public boolean isFinish() {
        return isFinish;
    }

    public void setFinish(boolean isFinish) {
        this.isFinish = isFinish;
    }

    public int getPos() {
        return pos;
    }

    public void setPos(int pos) {
        this.pos = pos;
    }

}
