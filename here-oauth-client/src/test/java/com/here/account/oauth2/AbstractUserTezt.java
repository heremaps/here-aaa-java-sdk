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

import java.io.IOException;

import org.junit.After;
import org.junit.Before;

import com.here.account.bo.AuthenticationHttpException;
import com.here.account.bo.AuthenticationRuntimeException;
import com.here.account.http.HttpException;
import com.here.account.http.HttpProvider;
import com.here.account.http.apache.ApacheHttpClientProvider;

public class AbstractUserTezt extends AbstractCredentialTezt {
    
    SignIn signIn;
    private int connectionTimeoutInMs = 10000;
    private int requestTimeoutInMs = 10000;
    HttpProvider httpProvider;
    String email;
    String password;
    
    @Before
    public void setUp() throws AuthenticationRuntimeException, IOException, AuthenticationHttpException, HttpException {
        super.setUp();
        
        email = System.getProperty("email");
        password = System.getProperty("password");

        
        httpProvider = ApacheHttpClientProvider.builder()
        .setConnectionTimeoutInMs(connectionTimeoutInMs)
        .setRequestTimeoutInMs(requestTimeoutInMs)
        .build();
        this.signIn = HereAccessTokenProviders
                .getSignIn(httpProvider, urlStart, clientId, clientSecret);
    }
    
    @After
    public void tearDown() throws IOException {
        if (null != httpProvider) {
            httpProvider.close();
        }
    }

}
