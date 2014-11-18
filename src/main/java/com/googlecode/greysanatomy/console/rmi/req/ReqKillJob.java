package com.googlecode.greysanatomy.console.rmi.req;


/**
 * «Î«Û…±À¿»ŒŒÒ
 *
 * @author chengtongda
 */
public class ReqKillJob extends GaRequest {
    private static final long serialVersionUID = 7156731632312708537L;

<<<<<<< HEAD
    private final String jobId;

    public ReqKillJob(long sessionId, String jobId) {
=======
    private final int jobId;

    public ReqKillJob(long sessionId, int jobId) {
>>>>>>> pr/8
        setGaSessionId(sessionId);
        this.jobId = jobId;
    }

<<<<<<< HEAD
    public String getJobId() {
=======
    public int getJobId() {
>>>>>>> pr/8
        return jobId;
    }

}
