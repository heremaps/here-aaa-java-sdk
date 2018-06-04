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
import java.util.logging.Level;
import java.util.logging.Logger;

import com.here.account.auth.NoAuthorizer;
import com.here.account.auth.provider.ClientAuthorizationProviderChain;
import com.here.account.client.Client;
import com.here.account.http.HttpConstants;
import com.here.account.http.HttpConstants.HttpMethods;
import com.here.account.http.HttpProvider;
import com.here.account.oauth2.bo.TimestampResponse;
import com.here.account.util.*;

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
     * @param clientAuthorizationRequestProvider identifies the token endpoint URL and
     *     client credentials to be injected into requests
     * @return a {@code TokenEndpoint} representing access for the provided client
     */
    public static TokenEndpoint getTokenEndpoint(
            HttpProvider httpProvider,
            ClientAuthorizationRequestProvider clientAuthorizationRequestProvider) {
        return getTokenEndpoint(
                reuseClock(clientAuthorizationRequestProvider),
                httpProvider, clientAuthorizationRequestProvider, new JacksonSerializer());
    }

    /**
     * If we can re-use the Clock, then corrections made by HereAccount will agree
     * with the ones the clientAuthorizationRequestProvider/OAuth1Signer uses.
     * Otherwise, if clientAuthorizationRequestProvider is null, or its clock is
     * null, a SettableSystemClock is returned.
     *
     * @param clientAuthorizationRequestProvider the authorization provider
     * @return the clock to use
     */
    private static Clock reuseClock(ClientAuthorizationRequestProvider clientAuthorizationRequestProvider) {
        Clock clock = null;
        if (null != clientAuthorizationRequestProvider) {
            clock = clientAuthorizationRequestProvider.getClock();
        }
        if (null == clock) {
            clock = new SettableSystemClock();
        }
        return clock;
    }

    public static TokenEndpoint getTokenEndpoint(
                                                 HttpProvider httpProvider,
                                                 ClientAuthorizationRequestProvider clientAuthorizationRequestProvider,
                                                 Serializer serializer) {
        return getTokenEndpoint(reuseClock(clientAuthorizationRequestProvider),
                httpProvider, clientAuthorizationRequestProvider, serializer);
    }


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
     * @param clock the clock implementation to use
     * @param httpProvider the HTTP-layer provider implementation
     * @param clientCredentialsProvider identifies the token endpoint URL and
     *     client credentials to be injected into requests
     * @param serializer the Serializer to use
     * @return a {@code TokenEndpoint} representing access for the provided client
     */
    private static TokenEndpoint getTokenEndpoint(Clock clock,
                                                 HttpProvider httpProvider,
            ClientAuthorizationRequestProvider clientCredentialsProvider,
            Serializer serializer) {
        return new TokenEndpointImpl(clock,
                httpProvider, clientCredentialsProvider, serializer);
    }
    
    /**
     * Get a RefreshableResponseProvider where when you invoke 
     * {@link RefreshableResponseProvider#getUnexpiredResponse()}, 
     * you will always get a current HERE Access Token, 
     * for the grant_type=client_credentials use case, for 
     * confidential clients.
     *
     * @param clock the clock to use
     * @param tokenEndpoint the token endpoint to request tokens
     * @param accessTokenRequestFactory the Supplier of AccessTokenRequests
     * @return the refreshable response provider presenting an always "fresh" client_credentials-based HERE Access Token.
     * @throws AccessTokenException if you had trouble authenticating your request to the authorization server, 
     *      or the authorization server rejected your request
     * @throws RequestExecutionException if trouble processing the request
     * @throws ResponseParsingException if trouble parsing the response
     */
    private static RefreshableResponseProvider<AccessTokenResponse> getRefreshableClientTokenProvider(
            Clock clock,
            TokenEndpoint tokenEndpoint, Supplier<AccessTokenRequest> accessTokenRequestFactory)
            throws AccessTokenException, RequestExecutionException, ResponseParsingException {
        return new RefreshableResponseProvider<>(
                clock,
                null,
                tokenEndpoint.requestToken(accessTokenRequestFactory.get()),
                (AccessTokenResponse previous) -> {
                    try {
                        return tokenEndpoint.requestToken(accessTokenRequestFactory.get());
                    } catch (AccessTokenException | RequestExecutionException | ResponseParsingException e) {
                        throw new RuntimeException("trouble refresh: " + e, e);
                    }
                },
                RefreshableResponseProvider.getScheduledExecutorServiceSize1()
        );
    }
    
    /**
     * Implementation of {@link TokenEndpoint}.
     */
    private static class TokenEndpointImpl implements TokenEndpoint {
        private static final Logger LOGGER = Logger.getLogger(TokenEndpointImpl.class.getName());


        /**
         * @deprecated to be removed.
         */
        @Deprecated
        public static final String HTTP_METHOD_POST = "POST";

        private final boolean currentTimeMillisSettable;
        private final Clock clock;
        private final SettableClock settableClock;
        private final String timestampUrl;
        private final boolean requestTokenFromFile;

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
        private TokenEndpointImpl(
                Clock clock,
                HttpProvider httpProvider,
                ClientAuthorizationRequestProvider clientAuthorizationProvider,
                Serializer serializer) {
            // these values are fixed once selected
            this.clock = clock;
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

            requestTokenFromFile = null != url && url.startsWith(FILE_URL_START);

            if (currentTimeMillisSettable = clock instanceof SettableClock
                    && null != url && url.endsWith(SLASH_TOKEN)) {
                settableClock = (SettableClock) clock;
                timestampUrl = url.substring(0, url.length() - SLASH_TOKEN.length()) + SLASH_TIMESTAMP;
            } else {
                settableClock = null;
                timestampUrl = null;
            }

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

        @Override
        public AccessTokenResponse requestToken(AccessTokenRequest authorizationRequest) 
                throws AccessTokenException, RequestExecutionException, ResponseParsingException {            
            if (requestTokenFromFile) {
                return requestTokenFromFile();
            } else {
                return requestTokenHttp(authorizationRequest, 1);
            }
        }
        
        protected AccessTokenResponse requestTokenHttp(AccessTokenRequest authorizationRequest,
                                                       int retryFixableErrorsCount)
                throws AccessTokenException, RequestExecutionException, ResponseParsingException {            
            String method = httpMethod.getMethod();
            
            HttpProvider.HttpRequest httpRequest;
            // OAuth2.0 uses application/x-www-form-urlencoded
            httpRequest = httpProvider.getRequest(
                clientAuthorizer, method, url, authorizationRequest.toFormParams());

            try {
                return client.sendMessage(httpRequest, AccessTokenResponse.class,
                        ErrorResponse.class, (statusCode, errorResponse) -> {
                            return new AccessTokenException(statusCode, errorResponse);
                        });
            } catch (AccessTokenException e) {
                return handleFixableErrors(authorizationRequest, retryFixableErrorsCount, e);
            }
        }

        private static final int CLOCK_SKEW_STATUS_CODE = 401;
        private static final int CLOCK_SKEW_ERROR_CODE = 401204;
        private static final long CONVERT_SECONDS_TO_MILLISECONDS = 1000L;

        private static final String SLASH_TOKEN = "/oauth2/token";
        private static final String SLASH_TIMESTAMP = "/timestamp";
        private final NoAuthorizer noAuthorizer = new NoAuthorizer();

        protected boolean canFixClockSkew(int retryFixableErrorsCount,
                                          AccessTokenException e) {
            ErrorResponse errorResponse;
            return currentTimeMillisSettable
                    && retryFixableErrorsCount > 0
                    && null != e && CLOCK_SKEW_STATUS_CODE == e.getStatusCode()
                    && null != (errorResponse = e.getErrorResponse())
                    && CLOCK_SKEW_ERROR_CODE == errorResponse.getErrorCode();
        }

        protected TimestampResponse getServerTimestamp() {
            // we have a clock skew
            String method = HttpConstants.HttpMethods.GET.getMethod();

            HttpProvider.HttpRequest httpRequest;
            httpRequest = httpProvider.getRequest(
                    noAuthorizer, method, timestampUrl, (String) null);

            TimestampResponse timestampResponse = client.sendMessage(httpRequest, TimestampResponse.class,
                    ErrorResponse.class, (statusCode, errorResponse2) -> {
                        return new AccessTokenException(statusCode, errorResponse2);
                    });

            return timestampResponse;
        }

        protected AccessTokenResponse handleFixableErrors(AccessTokenRequest authorizationRequest,
                                                          int retryFixableErrorsCount,
                                                          AccessTokenException e) {
            if (canFixClockSkew(retryFixableErrorsCount, e)) {
                // correct the Clock
                try {
                    TimestampResponse timestampResponse = getServerTimestamp();
                    long timestamp = timestampResponse.getTimestamp();
                    settableClock.setCurrentTimeMillis(timestamp * CONVERT_SECONDS_TO_MILLISECONDS);
                } catch (Exception e2) {
                    // trouble correcting the clock
                    LOGGER.warning(() -> "correcting clock skew, trouble getting timestamp: " + e2);
                    throw e;
                }

                // retry
                return requestTokenHttp(authorizationRequest, retryFixableErrorsCount - 1);

            }
            throw e;
        }
        
        //@Override
        public Fresh<AccessTokenResponse> requestAutoRefreshingToken(Supplier<AccessTokenRequest> requestSupplier) 
                throws AccessTokenException, RequestExecutionException, ResponseParsingException {
            final RefreshableResponseProvider<AccessTokenResponse> refresher = 
                    HereAccount.getRefreshableClientTokenProvider(clock, this, requestSupplier);
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
