/*
 * Copyright (c) 2016 HERE Europe B.V.
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
package com.here.account.oauth2;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * The POJO representation of an OAuth2.0 HERE authorization server error response.
 * See also the 
 * OAuth2.0 <a href="https://tools.ietf.org/html/rfc6749#section-5.2">Error Response</a> 
 * section.
 * 
 * @author kmccrack
 *
 */
public class ErrorResponse {
    
    /**
     * The error value.
     * From OAuth2.0 <a href="https://tools.ietf.org/html/rfc6749#section-5.2">Error Response</a> 
     * section, the following parameter: 
     * error
         REQUIRED.  A single ASCII [USASCII] error code from the
         following:

         invalid_request
               The request is missing a required parameter, includes an
               unsupported parameter value (other than grant type),
               repeats a parameter, includes multiple credentials,
               utilizes more than one mechanism for authenticating the
               client, or is otherwise malformed.

         invalid_client
               Client authentication failed (e.g., unknown client, no
               client authentication included, or unsupported
               authentication method).  The authorization server MAY
               return an HTTP 401 (Unauthorized) status code to indicate
               which HTTP authentication schemes are supported.  If the
               client attempted to authenticate via the "Authorization"
               request header field, the authorization server MUST
               respond with an HTTP 401 (Unauthorized) status code and
               include the "WWW-Authenticate" response header field
               matching the authentication scheme used by the client.

         invalid_grant
               The provided authorization grant (e.g., authorization
               code, resource owner credentials) or refresh token is
               invalid, expired, revoked, does not match the redirection
               URI used in the authorization request, or was issued to
               another client.

         unauthorized_client
               The authenticated client is not authorized to use this
               authorization grant type.

         unsupported_grant_type
               The authorization grant type is not supported by the
               authorization server.

         invalid_scope
               The requested scope is invalid, unknown, malformed, or
               exceeds the scope granted by the resource owner.

         Values for the "error" parameter MUST NOT include characters
         outside the set %x20-21 / %x23-5B / %x5D-7E.
     * 
     */
    private final String error;
    
    /**
     * The error_description value.
     * From OAuth2.0 <a href="https://tools.ietf.org/html/rfc6749#section-5.2">Error Response</a> 
     * section, the following parameter: 
     * error_description
         OPTIONAL.  Human-readable ASCII [USASCII] text providing
         additional information, used to assist the client developer in
         understanding the error that occurred.
         Values for the "error_description" parameter MUST NOT include
         characters outside the set %x20-21 / %x23-5B / %x5D-7E.
     */
    @JsonProperty("error_description")
    private final String errorDescription;
    
    /**
     * The numeric HTTP status code.
     * See also the HTTP 
     * <a href="https://tools.ietf.org/html/rfc7231#section-6">Response Status Codes</a> 
     * section.
     * 
     * <p>
     * This property is a HERE extension to RFC6749 providing additional data.
     */
    private final Integer httpStatus;
    
    /**
     * A unique error identifier, useful for support questions, 
     * or tracking purposes.
     * 
     * <p>
     * This property is a HERE extension to RFC6749 providing additional data.
     */
    private final String errorId;
    
    /**
     * A more detailed categorized code for the error, specific to the HERE authorization server 
     * semantics.
     * 
     * <p>
     * This property is a HERE extension to RFC6749 providing additional data.
     */
    private final Integer errorCode;
    
    /**
     * A potentially-human-readable message describing the error.
     * No machine or automated code should process or interpret this value.
     * 
     * <p>
     * This property is a HERE extension to RFC6749 providing additional data.
     */
    private final String message;
    
    public ErrorResponse() {
        this(null, null, null, null, null, null);
    }
    
    public ErrorResponse(String error, 
            String errorDescription,
          String errorId,
          Integer httpStatus,
          Integer errorCode,
          String message) {
        this.error = error;
        this.errorDescription = errorDescription;
        this.httpStatus = httpStatus;
        this.errorId = errorId;
        this.errorCode = errorCode;
        this.message = message;
    }
    
    /**
     * The error value.
     * From OAuth2.0 <a href="https://tools.ietf.org/html/rfc6749#section-5.2">Error Response</a> 
     * section, the following parameter: 
     * error
         REQUIRED.  A single ASCII [USASCII] error code from the
         following:

         invalid_request
               The request is missing a required parameter, includes an
               unsupported parameter value (other than grant type),
               repeats a parameter, includes multiple credentials,
               utilizes more than one mechanism for authenticating the
               client, or is otherwise malformed.

         invalid_client
               Client authentication failed (e.g., unknown client, no
               client authentication included, or unsupported
               authentication method).  The authorization server MAY
               return an HTTP 401 (Unauthorized) status code to indicate
               which HTTP authentication schemes are supported.  If the
               client attempted to authenticate via the "Authorization"
               request header field, the authorization server MUST
               respond with an HTTP 401 (Unauthorized) status code and
               include the "WWW-Authenticate" response header field
               matching the authentication scheme used by the client.

         invalid_grant
               The provided authorization grant (e.g., authorization
               code, resource owner credentials) or refresh token is
               invalid, expired, revoked, does not match the redirection
               URI used in the authorization request, or was issued to
               another client.

         unauthorized_client
               The authenticated client is not authorized to use this
               authorization grant type.

         unsupported_grant_type
               The authorization grant type is not supported by the
               authorization server.

         invalid_scope
               The requested scope is invalid, unknown, malformed, or
               exceeds the scope granted by the resource owner.

         Values for the "error" parameter MUST NOT include characters
         outside the set %x20-21 / %x23-5B / %x5D-7E.
     * 
     * @return the error
     */
    public String getError() {
        return error;
    }

    /**
     * The error_description value.
     * From OAuth2.0 <a href="https://tools.ietf.org/html/rfc6749#section-5.2">Error Response</a> 
     * section, the following parameter: 
     * error_description
         OPTIONAL.  Human-readable ASCII [USASCII] text providing
         additional information, used to assist the client developer in
         understanding the error that occurred.
         Values for the "error_description" parameter MUST NOT include
         characters outside the set %x20-21 / %x23-5B / %x5D-7E.
     * 
     * @return the error description
     */
    public String getErrorDescription() {
        return errorDescription;
    }
    
    /**
     * The httpStatus.
     * The numeric HTTP status code.
     * See also the HTTP 
     * <a href="https://tools.ietf.org/html/rfc7231#section-6">Response Status Codes</a> 
     * section.
     * 
     * <p>
     * This property is a HERE extension to RFC6749 providing additional data.
     * 
     * @return the httpStatus
     */
    public Integer getHttpStatus() {
       return httpStatus;
    }

    /**
     * The errorId.
     * A unique error identifier, useful for support questions, 
     * or tracking purposes.
     * 
     * <p>
     * This property is a HERE extension to RFC6749 providing additional data.
     * 
     * @return the errorId
     */
    public String getErrorId() {
        return errorId;
    }

    /**
     * The errorCode.
     * A more detailed categorized code for the error, specific to the HERE authorization server 
     * semantics.
     * 
     * <p>
     * This property is a HERE extension to RFC6749 providing additional data.
     *
     * @return the errorCode
     */
    public Integer getErrorCode() {
        return errorCode;
    }

    /**
     * The message.
     * A potentially-human-readable message describing the error.
     * No machine or automated code should process or interpret this value.
     * 
     * <p>
     * This property is a HERE extension to RFC6749 providing additional data.
     * 
     * @return the message
     */
    public String getMessage() {
        return message;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return "ErrorResponse [error=" + error + ", errorDescription=" + errorDescription + ", httpStatus=" + httpStatus
                + ", errorId=" + errorId + ", errorCode=" + errorCode + ", message=" + message + "]";
    }
        
}
