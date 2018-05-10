/*
 * Copyright (c) 2018 HERE Europe B.V.
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
package com.here.account.client;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.function.BiFunction;

import com.here.account.http.HttpException;
import com.here.account.http.HttpProvider;
import com.here.account.http.HttpProvider.HttpRequest;
import com.here.account.oauth2.AccessTokenException;
import com.here.account.oauth2.RequestExecutionException;
import com.here.account.oauth2.ResponseParsingException;
import com.here.account.util.Serializer;

/**
 * A Client that talks to a Resource Server, in OAuth2-speak.
 * It is expected that a wrapper class invokes methods on an instance 
 * of this class, so that simple Java create(), read(), update(), 
 * and delete() methods can be written with POJOs.
 * 
 * Suitable for use with JSON-object response APIs.
 * 
 * @author kmccrack
 */
public class Client {

    public static class Builder {
        private HttpProvider httpProvider;
        private Serializer serializer;
        private HttpProvider.HttpRequestAuthorizer clientAuthorizer;

        private Builder() {

        }

        public Builder withHttpProvider(HttpProvider httpProvider) {
            this.httpProvider = httpProvider;
            return this;
        }

        public Builder withSerializer(Serializer serializer) {
            this.serializer = serializer;
            return this;
        }

        public Builder withClientAuthorizer(HttpProvider.HttpRequestAuthorizer clientAuthorizer) {
            this.clientAuthorizer = clientAuthorizer;
            return this;
        }

        public Client build() {
            return new Client(httpProvider, serializer, clientAuthorizer);
        }
    }

    public static Builder builder() {
        return new Builder();
    }

    private final HttpProvider httpProvider;
    private final Serializer serializer;
    private final HttpProvider.HttpRequestAuthorizer clientAuthorizer;

    private Client(HttpProvider httpProvider, Serializer serializer,
                    HttpProvider.HttpRequestAuthorizer clientAuthorizer) {
        this.httpProvider = httpProvider;
        this.serializer = serializer;
        this.clientAuthorizer = clientAuthorizer;
    }

    /**
     * Sends the requested HTTP Message to the Server.
     * This method is useful if you want to interact with your 
     * Resource Server using serializable Java objects as inputs and outputs.
     * It covers the case where your Resource Server API uses 
     * Content-Type: application/json for request and response 
     * documents.
     *
     * @param method the HTTP method
     * @param url the HTTP request URL
     * @param request the request object of type R, or null if no request object
     * @param responseClass the response object class, for deserialization
     * @param errorResponseClass the response error object class, for deserialization
     * @param newExceptionFunction the function for getting a new RuntimeException based
     *      on the statusCode and error response object
     * @param <R> the Request parameterized type
     * @param <T> the Response parameterized type
     * @param <U> the Response Error parameterized type
     * @return the Response of type T
     * @throws RequestExecutionException if trouble executing the request
     * @throws ResponseParsingException if trouble serializing the request, 
     *      or deserializing the response
     */
    public <R, T, U> T sendMessage(
            String method,
            String url,
            R request,
            Class<T> responseClass,
            Class<U> errorResponseClass,
            BiFunction<Integer, U, RuntimeException> newExceptionFunction)
            throws RequestExecutionException, ResponseParsingException {

        HttpProvider.HttpRequest httpRequest;
        if (null == request) {
            httpRequest = httpProvider.getRequest(
                    clientAuthorizer, method, url, (String) null);
        } else {
            // HttpConstants.ContentTypes.JSON == requestContentType
            String jsonBody = serializer.objectToJson(request);
            httpRequest = httpProvider.getRequest(
                        clientAuthorizer, method, url, jsonBody);
        }

        return sendMessage(httpRequest, responseClass,
                errorResponseClass, newExceptionFunction);
    }
    
    /**
     * Sends the requested HTTP Message to the Server.
     * This method is useful if you have already constructed your 
     * HttpRequest, and your API supports 
     * Content-Type: application/json response documents.
     *
     * @param httpRequest the HTTP Request
     * @param responseClass the Response class
     * @param errorResponseClass the class for Error Responses
     * @param newExceptionFunction the new RuntimeException-creating function 
     *     that takes a statusCode and an Error Response object.
     * @param <T> the Response parameterized type
     * @param <U> the Response Error parameterized type
     * @return the Response of type T
     * @throws RequestExecutionException if trouble executing the request
     * @throws ResponseParsingException if trouble serializing the request, 
     *      or deserializing the response
     */
    public <T, U> T sendMessage(HttpRequest httpRequest, Class<T> responseClass,
            Class<U> errorResponseClass,
            BiFunction<Integer, U, RuntimeException> newExceptionFunction) 
            throws RequestExecutionException, ResponseParsingException {
        // blocking
        HttpProvider.HttpResponse httpResponse = null;
        InputStream jsonInputStream = null;

        try {
            httpResponse = httpProvider.execute(httpRequest);
            jsonInputStream = httpResponse.getResponseBody();
        } catch (IOException | HttpException e) {
            throw new RequestExecutionException(e);
        }

        int statusCode = httpResponse.getStatusCode();
        try {
            if (200 == statusCode || 201 == statusCode || 204 == statusCode) {
                try {
                    return serializer.jsonToPojo(jsonInputStream,
                            responseClass);
                } catch (Exception e) {
                    throw new ResponseParsingException(e);
                }
            } else {
                U errorResponse;
                try {
                    // parse the error response
                    errorResponse = serializer.jsonToPojo(jsonInputStream, errorResponseClass);
                } catch (Exception e) {
                    // if there is trouble parsing the error
                    throw new ResponseParsingException(e);
                }
                throw newExceptionFunction.apply(statusCode, errorResponse);
            }
        } finally {
            nullSafeCloseThrowingUnchecked(jsonInputStream);
        }
    }

    /**
     * A null-safe invocation of closeable.close(), such that if an IOException is 
     * triggered, it is wrapped instead in an UncheckedIOException.
     * 
     * @param closeable the closeable to be closed
     */
    static void nullSafeCloseThrowingUnchecked(Closeable closeable) {
        if (null != closeable) {
            try {
                closeable.close();
            } catch (IOException ioe) {
                throw new UncheckedIOException(ioe);
            }
        }

    }

}
