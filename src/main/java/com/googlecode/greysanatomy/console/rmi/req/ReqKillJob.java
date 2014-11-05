package com.googlecode.greysanatomy.console.rmi.req;


/**
 * «Î«Û…±À¿»ŒŒÒ
 *
 * @author chengtongda
 */
public class ReqKillJob extends GaRequest {
    private static final long serialVersionUID = 7156731632312708537L;

    private final int jobId;

    public ReqKillJob(long sessionId, int jobId) {
        setGaSessionId(sessionId);
        this.jobId = jobId;
    }

    public int getJobId() {
        return jobId;
    }

}
