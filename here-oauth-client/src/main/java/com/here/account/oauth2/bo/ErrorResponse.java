/*
 * Copyright 2016 HERE Global B.V.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.here.account.oauth2.bo;

/* Example:
com.here.account.bo.AuthenticationHttpException: HTTP status code 401, body 
{"errorId":"ERROR-905ffdd8-34b1-4fc7-ba98-775206d292f9","httpStatus":401,"hereErrorCode":401400,"errorCode":401400,"message":"Invalid Credentials for user:test4312@example.com"}
 */
/**
 * The POJO representation of a HERE authorization server error response.
 * See also the 
 * OAuth2.0 <a href="https://tools.ietf.org/html/rfc6749#section-5.2">Error Response</a> 
 * section.
 * 
 * @author kmccrack
 *
 */
public class ErrorResponse {

    /**
     * A unique error identifier, useful for support questions, 
     * or tracking purposes.
     */
    private final String errorId;
    
    /**
     * The numeric HTTP status code.
     * See also the HTTP 
     * <a href="https://tools.ietf.org/html/rfc7231#section-6">Response Status Codes</a> 
     * section.
     */
    private final Integer httpStatus;
    
    /**
     * A more detailed categorized code for the error, specific to the HERE authorization server 
     * semantics.
     */
    private final Integer errorCode;
    
    /**
     * A potentially-human-readable message describing the error.
     * No machine or automated code should process or interpret this value.
     */
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
     * A unique error identifier, useful for support questions, 
     * or tracking purposes.
     * 
     * @return the errorId
     */
    public String getErrorId() {
        return errorId;
    }

    /**
     * the httpStatus.
     * The numeric HTTP status code.
     * See also the HTTP 
     * <a href="https://tools.ietf.org/html/rfc7231#section-6">Response Status Codes</a> 
     * section.
     * 
     * @return the httpStatus
     */
    public Integer getHttpStatus() {
       return httpStatus;
    }

    /**
     * the errorCode.
     * A more detailed categorized code for the error, specific to the HERE authorization server 
     * semantics.
     * 
     * @return the errorCode
     */
    public Integer getErrorCode() {
        return errorCode;
    }

    /**
     * the message.
     * A potentially-human-readable message describing the error.
     * No machine or automated code should process or interpret this value.
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
