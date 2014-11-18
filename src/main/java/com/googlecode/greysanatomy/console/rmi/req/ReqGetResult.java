package com.googlecode.greysanatomy.console.rmi.req;


/**
 * 请求job执行结果
 *
 * @author chengtongda
 */
public class ReqGetResult extends GaRequest {
    private static final long serialVersionUID = 7156731632312708537L;

<<<<<<< HEAD
    private final String jobId;

    private final int pos;

    public ReqGetResult(String jobId, long sessionId, int pos) {
=======
    private final int jobId;

    private final int pos;

    public ReqGetResult(int jobId, long sessionId, int pos) {
>>>>>>> pr/8
        this.jobId = jobId;
        this.pos = pos;
        setGaSessionId(sessionId);
    }

<<<<<<< HEAD
    public String getJobId() {
=======
    public int getJobId() {
>>>>>>> pr/8
        return jobId;
    }

    public int getPos() {
        return pos;
    }

}
