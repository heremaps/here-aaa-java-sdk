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

import java.io.File;
import java.io.IOException;

import com.here.account.auth.OAuth1ClientCredentialsProvider;
import com.here.account.http.HttpException;
import com.here.account.http.HttpProvider;
import com.here.account.http.apache.ApacheHttpClientProvider;
import com.here.account.util.JsonSerializer;
import com.here.account.util.RefreshableResponseProvider;
import java.io.InputStream;
import java.io.UncheckedIOException;

/**
 * Static entry point to access HERE Account via the OAuth2.0 API.  This class
 * facilitates getting and maintaining a HERE
 * Access Token to use on requests to HERE Service REST APIs according to 
 * <a href="https://tools.ietf.org/html/rfc6750">The OAuth 2.0 Authorization Framework: Bearer Token Usage</a>.
 * See also the OAuth2.0 
 * <a href="https://tools.ietf.org/html/rfc6749#section-1.4">Access Token</a> spec.
 * 
 * <p>
 * Specific use cases currently supported include:
 * <ul>
 * <li>
 * Get a one time use HERE Access Token:
 * <pre>
 * {@code
        // set up url, accessKeyId, and accessKeySecret.
        TokenEndpoint tokenEndpoint = HereAccount.getTokenEndpoint(
                ApacheHttpClientProvider.builder().build(), 
                new OAuth1ClientCredentialsProvider(url, accessKeyId, accessKeySecret));
        
        String hereAccessToken = tokenEndpoint.requestToken(
                new ClientCredentialsGrantRequest()).getAccessToken();
        // use hereAccessToken on requests until expires...
   }
 * </pre>
 * </li>
 * <li>
 * Get an auto refreshing HERE Access Token:
 * <pre>
 * {@code
        // set up url, accessKeyId, and accessKeySecret.
        TokenEndpoint tokenEndpoint = HereAccount.getTokenEndpoint(
                ApacheHttpClientProvider.builder().build(), 
                new OAuth1ClientCredentialsProvider(url, accessKeyId, accessKeySecret));
        // call this once and keep a reference to freshToken, such as in your beans
        Fresh<AccessTokenResponse> freshToken = tokenEndpoint.requestAutoRefreshingToken(
                new ClientCredentialsGrantRequest());
        
        // using your reference to freshToken, for each request, just ask for the token
        // the same hereAccessToken is returned for most of the valid time; but as it nears 
        // expiry the returned value will change.
        String hereAccessToken = freshToken.get().getAccessToken();
        // use hereAccessToken on your request...
   }
 * </pre>
 * </li>
 * </ul>
 * 
 * <p>
 * Convenience {@link ClientCredentialsProvider} implementations are also available to
 * automatically pull the {@code url}, {@code accessKeyId}, and {@code accessKeySecret}
 * from a {@code Properties} object or properties file:
 * <ul>
 * <li>
 * Get configuration from properties file:
 * <pre>
 * {@code
        // setup url, accessKeyId, and accessKeySecret as properties in credentials.properties
        TokenEndpoint tokenEndpoint = HereAccount.getTokenEndpoint(
                ApacheHttpClientProvider.builder().build(), 
                new OAuth1ClientCredentialsProvider.FromFile(new File("credentials.properties")));
        // choose 
        //   tokenEndpoint.requestToken(new ClientCredentialsGrantRequest());
        // or 
        //   tokenEndpoint.requestAutoRefreshingToken(new ClientCredentialsGrantRequest());
   }
 * </pre>
 * </li>
 * 
 * </ul>
 */
public class HereAccount {
    
    /**
     * This class cannot be instantiated.
     */
    private HereAccount() {}

    /**
     * Get the ability to run various Token Endpoint API calls to the 
     * HERE Account Authorization Server.
     * See OAuth2.0 
     * <a href="https://tools.ietf.org/html/rfc6749#section-4">Obtaining Authorization</a>.
     * 
     * The returned {@code TokenEndpoint} exposes an abstraction to make calls
     * against the OAuth2 token endpoint identified by the given client credentials
     * provider.  In addition, all calls made against the returned endpoint will
     * automatically be injected with the given client credentials.
     * 
     * @param httpProvider the HTTP-layer provider implementation
     * @param clientCredentialsProvider identifies the token endpoint URL and
     *     client credentials to be injected into requests
     * @return a {@code TokenEndpoint} representing access for the provided client
     */
    public static TokenEndpoint getTokenEndpoint(
            HttpProvider httpProvider,
            ClientCredentialsProvider clientCredentialsProvider) {
        return new TokenEndpointImpl( httpProvider, clientCredentialsProvider);
    }
    
    /**
     * Get a RefreshableResponseProvider where when you invoke 
     * {@link RefreshableResponseProvider#getUnexpiredResponse()}, 
     * you will always get a current HERE Access Token, 
     * for the grant_type=client_credentials use case, for 
     * confidential clients.
     * 
     * @param tokenEndpoint the token endpoint to request tokens
     * @return the refreshable response provider presenting an always "fresh" client_credentials-based HERE Access Token.
     * @throws AccessTokenException if you had trouble authenticating your request to the authorization server, 
     *      or the authorization server rejected your request
     * @throws RequestExecutionException if trouble processing the request
     * @throws ResponseParsingException if trouble parsing the response
     */
    private static RefreshableResponseProvider<AccessTokenResponse> getRefreshableClientTokenProvider(
            TokenEndpoint tokenEndpoint) 
            throws AccessTokenException, RequestExecutionException, ResponseParsingException {
        return new RefreshableResponseProvider<>(
                null,
                tokenEndpoint.requestToken(new ClientCredentialsGrantRequest()),
                (AccessTokenResponse previous) -> {
                    try {
                        return tokenEndpoint.requestToken(new ClientCredentialsGrantRequest());
                    } catch (AccessTokenException | RequestExecutionException | ResponseParsingException e) {
                        throw new RuntimeException("trouble refresh: " + e, e);
                    }
                });
    }
    
    /**
     * Implementation of {@link TokenEndpoint}.
     */
    private static class TokenEndpointImpl implements TokenEndpoint {
        public static final String HTTP_METHOD_POST = "POST";
        
        private final HttpProvider httpProvider;
        private final String url;
        private final HttpProvider.HttpRequestAuthorizer clientAuthorizer;
        
        /**
         * Construct a new ability to obtain authorization from the HERE authorization server.
         * 
         * @param httpProvider the HTTP-layer provider implementation
         * @param urlStart the protocol, host, and port portion of the HERE authorization server endpoint you want to call.
         * @param clientId see also <a href="https://tools.ietf.org/html/rfc6749#section-2.3.1">client_id</a>; 
         *     as recommended by the RFC, we don't provide this in the body, but make it part of the request signature.
         * @param clientSecret see also <a href="https://tools.ietf.org/html/rfc6749#section-2.3.1">client_secret</a>; 
         *     as recommended by the RFC, we don't provide this in the body, but make it part of the request signature.
         */
        private TokenEndpointImpl(HttpProvider httpProvider, ClientCredentialsProvider clientCredentialsProvider) {
            this.httpProvider = httpProvider;
            this.url = clientCredentialsProvider.getTokenEndpointUrl();
            this.clientAuthorizer = clientCredentialsProvider.getClientAuthorizer();
        }

        @Override
        public AccessTokenResponse requestToken(AccessTokenRequest authorizationRequest) 
                throws AccessTokenException, RequestExecutionException, ResponseParsingException {
            String method = HTTP_METHOD_POST;
            
            // OAuth2.0 uses application/x-www-form-urlencoded
            HttpProvider.HttpRequest apacheRequest = httpProvider.getRequest(
                    clientAuthorizer, method, url, authorizationRequest.toFormParams());
            
            // blocking
            HttpProvider.HttpResponse apacheResponse = null;
            InputStream jsonInputStream = null;
            try {
                apacheResponse = httpProvider.execute(apacheRequest);
                jsonInputStream = apacheResponse.getResponseBody();
            } catch (IOException | HttpException e) {
                throw new RequestExecutionException(e);
            }
            
            int statusCode = apacheResponse.getStatusCode();
            try {
                if (200 == statusCode) {
                    try {
                        return JsonSerializer.toPojo(jsonInputStream,
                                                     AccessTokenResponse.class);
                    } catch (IOException ioe) {
                        throw new ResponseParsingException(ioe);
                    }
                } else {
                    try {
                        // parse the error response
                        ErrorResponse errorResponse = JsonSerializer.toPojo(jsonInputStream, ErrorResponse.class);
                        throw new AccessTokenException(statusCode, errorResponse);
                    } catch (IOException ioe) {
                        // if there is trouble parsing the error
                        throw new ResponseParsingException(ioe);
                    }
                }
            } finally {
                if (null != jsonInputStream) {
                    try {
                        jsonInputStream.close();
                    } catch (IOException ioe) {
                        throw new UncheckedIOException(ioe);
                    }
                }
            }
        }

        @Override
        public Fresh<AccessTokenResponse> requestAutoRefreshingToken(AccessTokenRequest request) 
                throws AccessTokenException, RequestExecutionException, ResponseParsingException {
            final RefreshableResponseProvider<AccessTokenResponse> refresher = HereAccount.getRefreshableClientTokenProvider(this);
            return () -> refresher.getUnexpiredResponse();
        }
        
    }
    
}
