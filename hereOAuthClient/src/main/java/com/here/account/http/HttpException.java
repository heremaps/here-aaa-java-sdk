package com.here.account.http;

public class HttpException extends Exception {

    /**
     * default.
     */
    private static final long serialVersionUID = 1L;

    public HttpException(String msg) {
        super(msg);
    }
    
    public HttpException(String msg, Throwable cause) {
        super(msg, cause);
    }
}
