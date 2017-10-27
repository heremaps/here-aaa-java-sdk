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
 * A ResponseParsingException occurs when there is a problem parsing a response
 * received from an HTTP request.
 */
public class ResponseParsingException extends RuntimeException {

    /**
     * Creates a new instance of <code>ResponseParsingException</code> without
     * detail message.
     */
    public ResponseParsingException() {
    }

    /**
     * Constructs an instance of <code>ResponseParsingException</code> with the
     * specified detail message.
     *
     * @param msg the detail message.
     */
    public ResponseParsingException(String msg) {
        super(msg);
    }
    
    /**
     * Constructs an instance of <code>ResponseParsingException</code> with the
     * specified detail message and cause.
     *
     * @param msg the detail message.
     * @param cause the cause
     */
    public ResponseParsingException(String msg, Throwable cause) {
        super(msg, cause);
    }
    
    /**
     * Constructs an instance of <code>ResponseParsingException</code> with the
     * specified cause.
     *
     * @param cause the cause
     */
    public ResponseParsingException(Throwable cause) {
        super(cause);
    }
}
