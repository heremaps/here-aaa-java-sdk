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

/**
 * A RequestExecutionException occurs when there is a problem processing a
 * request made to an HTTP endpoint.
 */
public class RequestExecutionException extends RuntimeException {

    /**
     * Creates a new instance of <code>RequestExecutionException</code> without
     * detail message.
     */
    public RequestExecutionException() {
    }

    /**
     * Constructs an instance of <code>RequestExecutionException</code> with the
     * specified detail message.
     *
     * @param msg the detail message.
     */
    public RequestExecutionException(String msg) {
        super(msg);
    }
    
    /**
     * Constructs an instance of <code>RequestExecutionException</code> with the
     * specified detail message and cause.
     *
     * @param msg the detail message.
     * @param cause the cause
     */
    public RequestExecutionException(String msg, Throwable cause) {
        super(msg, cause);
    }
    
    /**
     * Constructs an instance of <code>RequestExecutionException</code> with the
     * specified cause.
     *
     * @param cause the cause
     */
    public RequestExecutionException(Throwable cause) {
        super(cause);
    }
}
