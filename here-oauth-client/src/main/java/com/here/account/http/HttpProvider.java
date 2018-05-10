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
package com.here.account.http;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;

/**
 * An interface to the HTTP wireline provider implementation of your choosing.
 * 
 * 
 * @author kmccrack
 *
 */
public interface HttpProvider extends Closeable {
    
    /**
     * Wrapper for an HTTP request.
     */
    public static interface HttpRequest {
        
        /**
         * Add the Authorization header to this request, with the specified 
         * <tt>value</tt>.
         * See also 
         * <a href="https://tools.ietf.org/html/rfc7235#section-4.2">RFC 7235</a>.
         * 
         * @param value the value to add in the Authorization header
         */
        void addAuthorizationHeader(String value);
        
    }
    
    /**
     * Wrapper for authorizing HTTP requests.
     */
    public static interface HttpRequestAuthorizer {
        
        /**
         * Computes and adds a signature or token to the request as appropriate 
         * for the authentication or authorization scheme.
         * 
         * @param httpRequest the HttpRequest under construction, 
         *      to which to attach authorization
         * @param method the HTTP method 
         * @param url the URL of the request
         * @param formParams the 
         *      Content-Type: application/x-www-form-urlencoded
         *      form parameters.
         */
        void authorize(HttpRequest httpRequest, String method, String url, 
                Map<String, List<String>> formParams);

    }
    
    /**
     * Wrapper for HTTP responses.
     */
    public static interface HttpResponse {
        
        /**
         * Returns the HTTP response status code.
         * 
         * @return the HTTP response status code as an int
         */
        int getStatusCode();
        
        /**
         * Returns the HTTP Content-Length header value.
         * The content length of the response body.
         * 
         * @return the content length of the response body.
         */
        long getContentLength();
        
        /**
         * Get the response body, as an <tt>InputStream</tt>.
         * 
         * @return if there was a response entity, returns the InputStream reading bytes 
         *      from that response entity.
         *      otherwise, if there was no response entity, this method returns null.
         * @throws IOException if there is I/O trouble
         */
        InputStream getResponseBody() throws IOException;
        
    }

    /**
     * Gets the RequestBuilder, with the specified method, url, and requestBodyJson.
     * The Authorization header has already been set according to the 
     * httpRequestAuthorizer implementation.
     * 
     * @param httpRequestAuthorizer for adding the Authorization header value
     * @param method HTTP method value
     * @param url HTTP request URL
     * @param requestBodyJson the
     *      Content-Type: application/json
     *      JSON request body.
     * @return the HttpRequest object you can {@link #execute(HttpRequest)}.
     */
    HttpRequest getRequest(HttpRequestAuthorizer httpRequestAuthorizer, String method, String url, String requestBodyJson);
    
    /**
     * Gets the RequestBuilder, with the specified method, url, and formParams. 
     * The Authorization header has already been set according to the 
     * httpRequestAuthorizer implementation.
     * 
     * @param httpRequestAuthorizer for adding the Authorization header value
     * @param method HTTP method value
     * @param url HTTP request URL
     * @param formParams the 
     *      Content-Type: application/x-www-form-urlencoded
     *      form parameters.
     * @return the HttpRequest object you can {@link #execute(HttpRequest)}.
     */
    HttpRequest getRequest(HttpRequestAuthorizer httpRequestAuthorizer, String method, String url,
            Map<String, List<String>> formParams);
    
    /**
     * Execute the <tt>httpRequest</tt>.
     * Implementing classes would commonly invoke or schedule a RESTful HTTPS API call 
     * over the wire to the configured Service as part of <tt>execute</tt>.
     * 
     * @param httpRequest the HttpRequest
     * @return the HttpResponse to the request
     * @throws HttpException if there is trouble executing the httpRequest
     * @throws IOException if there is I/O trouble executing the httpRequest
     */
    HttpResponse execute(HttpRequest httpRequest) throws HttpException, IOException;
    
}
