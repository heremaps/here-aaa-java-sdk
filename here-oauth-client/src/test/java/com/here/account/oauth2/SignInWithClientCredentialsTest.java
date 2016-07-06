/*
 * Copyright 2016 HERE Global B.V.
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

import java.io.IOException;
import java.util.concurrent.ExecutionException;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.here.account.bo.AuthenticationHttpException;
import com.here.account.bo.AuthenticationRuntimeException;
import com.here.account.http.HttpConstants;
import com.here.account.http.HttpException;
import com.here.account.http.HttpProvider;
import com.here.account.http.apache.ApacheHttpClientProvider;
import com.here.account.oauth2.bo.ClientCredentialsGrantRequest;
import com.here.account.oauth2.bo.ErrorResponse;

public class SignInWithClientCredentialsTest extends AbstractCredentialTezt {

    HttpProvider httpProvider;
    SignIn signIn;
    
    @Before
    public void setUp() throws AuthenticationRuntimeException, IOException, AuthenticationHttpException, HttpException {
        super.setUp();
        //    public SignInWithClientCredentials(String urlStart, String clientId, String clientSecret) {
        
        httpProvider = ApacheHttpClientProvider.builder()
        .setConnectionTimeoutInMs(HttpConstants.DEFAULT_CONNECTION_TIMEOUT_IN_MS)
        .setRequestTimeoutInMs(HttpConstants.DEFAULT_REQUEST_TIMEOUT_IN_MS)
        .build();
        
        this.signIn = new SignIn(
                httpProvider,
                urlStart, clientId, clientSecret
                );
    }
    
    @After
    public void tearDown() throws IOException {
        if (null != httpProvider) {
            httpProvider.close();
        }
    }

    @Test
    public void test_signIn() throws IOException, InterruptedException, ExecutionException, AuthenticationHttpException, AuthenticationRuntimeException, HttpException {
        String hereAccessToken = signIn.signIn(new ClientCredentialsGrantRequest()).getAccessToken();
        assertTrue("hereAccessToken was null or blank", null != hereAccessToken && hereAccessToken.length() > 0);
    }
    
    @Test
    public void test_signIn_fatFinger() throws AuthenticationRuntimeException, IOException, HttpException {
        this.signIn = new SignIn(
                httpProvider,
                urlStart, clientId, "fat" + clientSecret
                );

        try{
            signIn.signIn(new ClientCredentialsGrantRequest()).getAccessToken();
        } catch (AuthenticationHttpException e) {
            ErrorResponse errorResponse = e.getErrorResponse();
            assertTrue("errorResponse was null", null != errorResponse);
            Integer errorCode = errorResponse.getErrorCode();
            Integer expectedErrorCode = 401300;
            assertTrue("errorCode was expected "+expectedErrorCode+", actual "+errorCode, expectedErrorCode.equals(errorCode));
        }

    }
}
