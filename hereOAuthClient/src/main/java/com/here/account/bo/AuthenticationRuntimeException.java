package com.here.account.bo;

public class AuthenticationRuntimeException extends RuntimeException {

    /**
     * default
     */
    private static final long serialVersionUID = 1L;

    public AuthenticationRuntimeException(String msg) {
        super(msg);
    }
    
    public AuthenticationRuntimeException(String msg, Exception cause) {
        super(msg, cause);
    }
}
