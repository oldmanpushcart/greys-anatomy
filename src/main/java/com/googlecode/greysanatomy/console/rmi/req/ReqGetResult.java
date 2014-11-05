package com.googlecode.greysanatomy.console.rmi.req;


/**
 * 请求job执行结果
 *
 * @author chengtongda
 */
public class ReqGetResult extends GaRequest {
    private static final long serialVersionUID = 7156731632312708537L;

    private final int jobId;

    private final int pos;

    public ReqGetResult(int jobId, long sessionId, int pos) {
        this.jobId = jobId;
        this.pos = pos;
        setGaSessionId(sessionId);
    }

    public int getJobId() {
        return jobId;
    }

    public int getPos() {
        return pos;
    }

}
