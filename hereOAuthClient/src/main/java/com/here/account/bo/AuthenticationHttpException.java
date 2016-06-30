package com.here.account.bo;

import com.here.account.oauth2.bo.ErrorResponse;

/**
 * If you had trouble authenticating, and got an HTTP response, 
 * you get an AuthenticationException.
 * 
 * @author kmccrack
 *
 */
public class AuthenticationHttpException extends Exception {

    /**
     * default.
     */
    private static final long serialVersionUID = 1L;
    
    private final int statusCode;
    private final ErrorResponse errorResponse;
    
    public AuthenticationHttpException(int statusCode, ErrorResponse errorResponse) {
        super("HTTP status code " + statusCode + ", body " + errorResponse);
        this.statusCode = statusCode;
        this.errorResponse = errorResponse;
    }

    public int getStatusCode() {
        return statusCode;
    }

    public ErrorResponse getErrorResponse() {
        return errorResponse;
    }

}
