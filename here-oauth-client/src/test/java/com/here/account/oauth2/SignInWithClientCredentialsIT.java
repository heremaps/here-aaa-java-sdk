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

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.concurrent.ScheduledExecutorService;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.here.account.auth.OAuth1ClientCredentialsProvider;
import com.here.account.auth.OAuth1Signer;
import com.here.account.http.HttpProvider;
import com.here.account.http.HttpConstants.HttpMethods;
import com.here.account.http.HttpProvider.HttpRequestAuthorizer;
import com.here.account.util.Clock;

public class SignInWithClientCredentialsIT extends AbstractCredentialTezt {

    HttpProvider httpProvider;
    TokenEndpoint signIn;
    
    @Before
    public void setUp() throws Exception {
        super.setUp();
        
        httpProvider = getHttpProvider();
        
        this.signIn = HereAccount.getTokenEndpoint(
                httpProvider,
                new OAuth1ClientCredentialsProvider(url, accessKeyId, accessKeySecret)
        );
    }
    
    @After
    public void tearDown() throws IOException {
        if (null != httpProvider) {
            httpProvider.close();
        }
    }

    @Test
    public void test_signIn() throws Exception {
        String hereAccessToken = signIn.requestToken(new ClientCredentialsGrantRequest()).getAccessToken();
        assertTrue("hereAccessToken was null or blank", null != hereAccessToken && hereAccessToken.length() > 0);
    }
    
    private Clock clock;
    private OAuth1Signer oauth1Signer;
    
    private long clockCurrentTimeMillis = 0;
    
    protected void setUpCustomClock() throws NoSuchMethodException, SecurityException, InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException, NoSuchFieldException, ClassNotFoundException {
        this.clock = new Clock() {

            @Override
            public long currentTimeMillis() {
                // we can manipulate the clock via a simple member variable setter
                return clockCurrentTimeMillis;
            }

            @Override
            public void schedule(ScheduledExecutorService scheduledExecutorService, Runnable runnable,
                    long millisecondsInTheFutureToSchedule) {
                // no impl needed for this test
            }
            
        };
        this.oauth1Signer = new OAuth1Signer(clock, accessKeyId, accessKeySecret);
        
        ClientCredentialsProvider clientCredentialsProvider = new ClientCredentialsProvider() {

            @Override
            public String getTokenEndpointUrl() {
                return url;
            }

            @Override
            public HttpRequestAuthorizer getClientAuthorizer() {
                return oauth1Signer;
            }

            @Override
            public AccessTokenRequest getNewAccessTokenRequest() {
                return new ClientCredentialsGrantRequest();
            }
            
            /**
             * {@inheritDoc}
             */
            @Override
            public HttpMethods getHttpMethod() {
                return HttpMethods.POST;
            }

            @Override
            public Clock getClock() {
                return null;
            }

        };
        
        this.signIn = HereAccount.getTokenEndpoint(
                httpProvider,
                clientCredentialsProvider
        );
    }

    private static final int THIRTY_MINUTES_IN_MILLISECONDS = 30 * 60 * 1000;
    
    @Test
    public void test_signIn_wrongClock() throws Exception {
        setUpCustomClock();
        this.clockCurrentTimeMillis = System.currentTimeMillis() + THIRTY_MINUTES_IN_MILLISECONDS;

        try {
            signIn.requestToken(new ClientCredentialsGrantRequest()).getAccessToken();
            fail("expected an AccessTokenException for clock 30 minutes in the future");
        } catch (AccessTokenException e) {
            // httpStatus 401, errorCode 401204: Time stamp is outside the valid period.
            int statusCode = e.getStatusCode();
            final int expectedStatusCode = 401;
            assertTrue("wrong clock test: expected statusCode " + expectedStatusCode + ", actual " + statusCode, 
                    expectedStatusCode == statusCode);
            ErrorResponse errorResponse = e.getErrorResponse();
            int httpStatus = errorResponse.getHttpStatus();
            assertTrue("wrong clock test: expected httpStatus " + expectedStatusCode + ", actual " + httpStatus, 
                    expectedStatusCode == httpStatus);
            String error = errorResponse.getError();
            String expectedError = "invalid_request";
            assertTrue("expected error " + expectedError + ", actual " + error, expectedError.equals(error));
            String expectedErrorDescriptionContains = "timestamp";
            String errorDescription = errorResponse.getErrorDescription();
            assertTrue("expected error_description to contain " + errorDescription + ", actual " + errorDescription, 
                    null != errorDescription && errorDescription.contains(expectedErrorDescriptionContains));
            int expectedErrorCode = 401204;
            Integer errorCode = errorResponse.getErrorCode();
            assertTrue("expected errorCode " + expectedErrorCode + ", actual " + errorCode, 
                    null != errorCode && errorCode.intValue() == expectedErrorCode);
        }

    }
    
    @Test
    public void test_signIn_fatFinger() throws Exception {
        this.signIn = HereAccount.getTokenEndpoint(
                httpProvider,
                new OAuth1ClientCredentialsProvider(url, accessKeyId, "fat" + accessKeySecret)
        );

        try{
            signIn.requestToken(new ClientCredentialsGrantRequest()).getAccessToken();
        } catch (AccessTokenException e) {
            ErrorResponse errorResponse = e.getErrorResponse();
            assertTrue("errorResponse was null", null != errorResponse);
            Integer errorCode = errorResponse.getErrorCode();
            Integer expectedErrorCode = 401300;
            assertTrue("errorCode was expected "+expectedErrorCode+", actual "+errorCode, expectedErrorCode.equals(errorCode));
        }

    }
    
    @Test
    public void test_signIn_expiresIn() throws Exception {
        AccessTokenResponse accessTokenResponse = signIn.requestToken(new ClientCredentialsGrantRequest());
        String hereAccessToken = accessTokenResponse.getAccessToken();
        assertTrue("hereAccessToken was null or blank", 
                null != hereAccessToken && hereAccessToken.length() > 0);
        
        AccessTokenResponse accessTokenResponse15 = signIn.requestToken(new ClientCredentialsGrantRequest().setExpiresIn(15L));
        String hereAccessToken15 = accessTokenResponse15.getAccessToken();
        assertTrue("hereAccessToken15 was null or blank", 
                null != hereAccessToken15 && hereAccessToken15.length() > 0);
    
        long expiresIn = accessTokenResponse.getExpiresIn();
        long expiresIn15 = accessTokenResponse15.getExpiresIn();
        assertTrue("expiresIn15 "+expiresIn15+" !< expiresIn "+expiresIn, 
                expiresIn15 < expiresIn);
        
        // Verification adds some tolerance to the exact number of seconds.
        // The server should not think it's valid longer than 15 seconds.
        // The client should have been successful telling the server this, 
        // and the client should be able to deserialize the response properly 
        // for this constraint to hold.
        assertTrue("expiresIn15 "+expiresIn15+" not in range",
                1 <= expiresIn15 && expiresIn15 <= 15);
    }
    
}
