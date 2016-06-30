package com.here.account.oauth2.bo;

/*
com.here.account.bo.AuthenticationHttpException: HTTP status code 401, body 
{"errorId":"ERROR-905ffdd8-34b1-4fc7-ba98-775206d292f9","httpStatus":401,"hereErrorCode":401400,"errorCode":401400,"message":"Invalid Credentials for user:test4312@example.com"}
 */
public class ErrorResponse {

    private final String errorId;
    private final Integer httpStatus;
    private final Integer errorCode;
    private final String message;
    
    public ErrorResponse() {
        this(null, null, null, null);
    }
    
    public ErrorResponse(String errorId,
      Integer httpStatus,
      Integer errorCode,
      String message) {
        this.errorId = errorId;
        this.httpStatus = httpStatus;
        this.errorCode = errorCode;
        this.message = message;
    }

    /**
     * the errorId.
     * 
     * @return the errorId
     */
    public String getErrorId() {
        return errorId;
    }

    /**
     * the httpStatus.
     * 
     * @return the httpStatus
     */
    public Integer getHttpStatus() {
       return httpStatus;
    }

    /**
     * the errorCode.
     * 
     * @return the errorCode
     */
    public Integer getErrorCode() {
        return errorCode;
    }

    /**
     * the message.
     * 
     * @return the message
     */
    public String getMessage() {
        return message;
    }

    @Override
    public String toString() {
        return "ErrorResponse [errorId=" + errorId + ", httpStatus=" + httpStatus + ", errorCode=" + errorCode
                + ", message=" + message + "]";
    }
        
}
