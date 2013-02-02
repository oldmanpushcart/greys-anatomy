package com.googlecode.greysanatomy.exception;

/**
 * session³¬Ê±
 * @author chengtongda
 *
 */
public class SessionTimeOutException extends Exception {
	
	private static final long serialVersionUID = 3533150161706718201L;

	public SessionTimeOutException() {
		super();
	}

	public SessionTimeOutException(String message, Throwable cause) {
		super(message, cause);
	}

	public SessionTimeOutException(String message) {
		super(message);
	}

	public SessionTimeOutException(Throwable cause) {
		super(cause);
	}
	
}
