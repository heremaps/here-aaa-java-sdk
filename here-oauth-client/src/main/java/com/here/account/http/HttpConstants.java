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

import java.nio.charset.Charset;

/**
 * Utility class which defines constants used in HTTP operations.
 */
public class HttpConstants {
    
    /**
     * This class cannot be instantiated.
     */
    private HttpConstants() {}

    public static final String AUTHORIZATION_HEADER = "Authorization";
    public static final String CHARSET_STRING = "UTF-8";
    public static final Charset ENCODING_CHARSET = Charset.forName(CHARSET_STRING);
    public static final String CONTENT_TYPE = "Content-Type";
    public static final String CONTENT_TYPE_JSON = "application/json";
    public static final String CONTENT_TYPE_FORM_URLENCODED = "application/x-www-form-urlencoded";
    public static final String CONTENT_TYPE_HEADER = "Content-Type";
    public static final String CONTENT_LENGTH_HEADER = "Content-Length";

    public static final int DEFAULT_REQUEST_TIMEOUT_IN_MS = 5000;
    public static final int DEFAULT_CONNECTION_TIMEOUT_IN_MS = 5000;

    public static enum HttpMethods {
        GET("GET"),
        POST("POST");
        
        private final String method;
        
        private HttpMethods(String method) {
            this.method = method;
        }
        
        /**
         * Returns the HTTP Method to be sent with the HTTP Request message.
         * 
         * @return the HTTP method
         */
        public String getMethod() {
            return method;
        }
    }
}
