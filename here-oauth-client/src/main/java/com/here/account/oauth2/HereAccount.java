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

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.URL;
import java.util.function.Supplier;

import com.here.account.client.Client;
import com.here.account.http.HttpConstants.HttpMethods;
import com.here.account.http.HttpProvider;
import com.here.account.util.JacksonSerializer;
import com.here.account.util.RefreshableResponseProvider;
import com.here.account.util.Serializer;

/**
 * Static entry point to access HERE Account via the OAuth2.0 API.  This class
 * facilitates getting and maintaining a HERE
 * Access Token to use on requests to HERE Service REST APIs according to 
 * <a href="https://tools.ietf.org/html/rfc6750">The OAuth 2.0 Authorization Framework: Bearer Token Usage</a>.
 * See also the OAuth2.0 
 * <a href="https://tools.ietf.org/html/rfc6749#section-1.4">Access Token</a> spec.
 * 
 * <p>
 * To use your provided credentials.properties to get a one-time use token:
 * <pre>
 * {@code
        // use your provided credentials.properties
        TokenEndpoint tokenEndpoint = HereAccount.getTokenEndpoint(
                ApacheHttpClientProvider.builder().build(), 
                new OAuth1ClientCredentialsProvider.FromFile(new File("credentials.properties")));
        
        String hereAccessToken = tokenEndpoint.requestToken(
                new ClientCredentialsGrantRequest()).getAccessToken();
        // use hereAccessToken on requests until expires...
   }
 * </pre>
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
 * </ul>
 * 
 * <p>
 * The above examples use the JavaHttpProvider.
 * Another example HttpProvider from this project below uses pure-Java.  
 * <pre>
 * {@code 
        // create a Java HttpProvider
        HttpProvider httpProvider = JavaHttpProvider.builder().build();
        // use httpProvider
 * }
 * </pre>
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
            ClientAuthorizationRequestProvider clientCredentialsProvider) {
        return getTokenEndpoint(httpProvider, clientCredentialsProvider, new JacksonSerializer());
    }
    
    public static TokenEndpoint getTokenEndpoint(HttpProvider httpProvider,
            ClientAuthorizationRequestProvider clientCredentialsProvider,
            Serializer serializer) {
        return new TokenEndpointImpl(httpProvider, clientCredentialsProvider, serializer);
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
            TokenEndpoint tokenEndpoint, Supplier<AccessTokenRequest> accessTokenRequestFactory) 
            throws AccessTokenException, RequestExecutionException, ResponseParsingException {
        return new RefreshableResponseProvider<>(
                null,
                tokenEndpoint.requestToken(accessTokenRequestFactory.get()),
                (AccessTokenResponse previous) -> {
                    try {
                        return tokenEndpoint.requestToken(accessTokenRequestFactory.get());
                    } catch (AccessTokenException | RequestExecutionException | ResponseParsingException e) {
                        throw new RuntimeException("trouble refresh: " + e, e);
                    }
                });
    }
    
    /**
     * Implementation of {@link TokenEndpoint}.
     */
    private static class TokenEndpointImpl implements TokenEndpoint {
        /**
         * @deprecated to be removed.
         */
        @Deprecated
        public static final String HTTP_METHOD_POST = "POST";
        
        private final Client client;
        private final HttpProvider httpProvider;
        private final HttpMethods httpMethod;
        private final String url;
        private final HttpProvider.HttpRequestAuthorizer clientAuthorizer;
        private final Serializer serializer;
        
        /**
         * Construct a new ability to obtain authorization from the HERE authorization server.
         * 
         * @param httpProvider the HTTP-layer provider implementation
         * @param clientAuthorizationProvider identifies a token endpoint,
         * provides a mechanism to use credentials to authorize access token requests,
         * and provides access token request objects
         * @param serializer used to serialize json To pojo and vice versa
         */
        private TokenEndpointImpl(HttpProvider httpProvider, ClientAuthorizationRequestProvider clientAuthorizationProvider,
                Serializer serializer) {
            // these values are fixed once selected
            this.url = clientAuthorizationProvider.getTokenEndpointUrl();
            this.clientAuthorizer = clientAuthorizationProvider.getClientAuthorizer();
            this.httpMethod = clientAuthorizationProvider.getHttpMethod();

            this.client = Client.builder()
                    .withHttpProvider(httpProvider)
                    .withClientAuthorizer(clientAuthorizer)
                    .withSerializer(serializer)
                    .build();
            this.httpProvider = httpProvider;
            this.serializer = serializer;
        }
        
        protected AccessTokenResponse requestTokenFromFile() 
                throws RequestExecutionException {
            try (InputStream is = new URL(url).openStream()){
                return serializer.jsonToPojo(is,
                        FileAccessTokenResponse.class);
            } catch (IOException e) {
                throw new RequestExecutionException(e);
            }
        }
        
        private static final String FILE_URL_START = "file://";
        
        protected boolean isRequestTokenFromFile() {
            return null != url && url.startsWith(FILE_URL_START);
        }
        
        @Override
        public AccessTokenResponse requestToken(AccessTokenRequest authorizationRequest) 
                throws AccessTokenException, RequestExecutionException, ResponseParsingException {            
            if (isRequestTokenFromFile()) {
                return requestTokenFromFile();
            } else {
                return requestTokenHttp(authorizationRequest);
            }
        }
        
        protected AccessTokenResponse requestTokenHttp(AccessTokenRequest authorizationRequest) 
                throws AccessTokenException, RequestExecutionException, ResponseParsingException {            
            String method = httpMethod.getMethod();
            
            HttpProvider.HttpRequest httpRequest;
            // OAuth2.0 uses application/x-www-form-urlencoded
            httpRequest = httpProvider.getRequest(
                clientAuthorizer, method, url, authorizationRequest.toFormParams());
            
            return client.sendMessage(httpRequest, AccessTokenResponse.class,
                    ErrorResponse.class, (statusCode, errorResponse) -> {
                        return new AccessTokenException(statusCode, errorResponse);                        
                    });
        }
        
        //@Override
        public Fresh<AccessTokenResponse> requestAutoRefreshingToken(Supplier<AccessTokenRequest> requestSupplier) 
                throws AccessTokenException, RequestExecutionException, ResponseParsingException {
            final RefreshableResponseProvider<AccessTokenResponse> refresher = 
                    HereAccount.getRefreshableClientTokenProvider(this, requestSupplier);
            return new Fresh<AccessTokenResponse>() {

                /**
                 * {@inheritDoc}
                 */
                @Override
                public AccessTokenResponse get() {
                    return refresher.getUnexpiredResponse();
                }

                /**
                 * {@inheritDoc}
                 */
                @Override
                public void close() throws IOException {
                    refresher.shutdown();
                }
            };
            
        }
        
        @Override
        public Fresh<AccessTokenResponse> requestAutoRefreshingToken(AccessTokenRequest request) 
                throws AccessTokenException, RequestExecutionException, ResponseParsingException {
            return requestAutoRefreshingToken(() -> {
                        return new ClientCredentialsGrantRequest();
                    });
        }
        
    }
   
    /**
     * A null-safe invocation of closeable.close(), such that if an IOException is 
     * triggered, it is wrapped instead in an UncheckedIOException.
     * 
     * @param closeable the closeable to be closed
     * @deprecated use Client method of the same name
     */
    @Deprecated
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
