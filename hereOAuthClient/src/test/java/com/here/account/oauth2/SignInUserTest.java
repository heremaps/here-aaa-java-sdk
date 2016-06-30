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
import static org.junit.Assert.fail;

import java.io.IOException;

import org.junit.Test;

import com.here.account.bo.AuthenticationHttpException;
import com.here.account.bo.AuthenticationRuntimeException;
import com.here.account.http.HttpException;
import com.here.account.oauth2.bo.AccessTokenResponse;
import com.here.account.oauth2.bo.ErrorResponse;
import com.here.account.oauth2.bo.PasswordGrantRequest;

public class SignInUserTest extends AbstractUserTezt {

    @Test
    public void test_userSignIn() throws AuthenticationRuntimeException, IOException, AuthenticationHttpException, HttpException {
        PasswordGrantRequest passwordGrantRequest = new PasswordGrantRequest().setEmail(email).setPassword(password);
        AccessTokenResponse accessTokenResponse = 
                signIn.signIn(passwordGrantRequest);
        assertTrue("accessTokenResponse was null", null != accessTokenResponse);
        String accessToken = accessTokenResponse.getAccessToken();
        assertTrue("accessToken should not have been blank", null != accessToken && accessToken.trim().length() > 0);
        String refreshToken = accessTokenResponse.getRefreshToken();
        assertTrue("refreshToken should not have been blank", null != refreshToken && refreshToken.trim().length() > 0);
        
    }
    
    @Test
    public void test_userSignIn_fatFinger() throws AuthenticationRuntimeException, IOException, AuthenticationHttpException, HttpException {
        PasswordGrantRequest passwordGrantRequest = new PasswordGrantRequest().setEmail(email).setPassword("fat"+password);
        try {
            signIn.signIn(passwordGrantRequest);
            fail("should have failed fat finger password");
        } catch (AuthenticationHttpException e) {
            ErrorResponse errorResponse = e.getErrorResponse();
            assertTrue("errorResponse was null", null != errorResponse);
            Integer errorCode = errorResponse.getErrorCode();
            Integer expectedErrorCode = 401400;
            assertTrue("errorCode was expected "+expectedErrorCode+", actual "+errorCode, 
                    errorCode == (int) expectedErrorCode);
        }
    }

}
