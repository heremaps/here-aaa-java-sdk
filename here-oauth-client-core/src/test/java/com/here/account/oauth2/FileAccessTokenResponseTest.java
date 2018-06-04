
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
package com.here.account.oauth2;

import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class FileAccessTokenResponseTest {

    FileAccessTokenResponse response;
    
    @Test
    public void test_expiresIn() {
        String accessToken = "my-access-token";
        String tokenType = null;
        Long expiresIn = null;
        String refreshToken = null;
        String idToken = null;
        int secondsFromNow = 45;
        Long exp = (System.currentTimeMillis() / 1000L) + secondsFromNow;
        
        response = new FileAccessTokenResponse( accessToken, 
                 tokenType,
                 expiresIn,  refreshToken,  idToken,
                 exp);
        
        String actualAccessToken = response.getAccessToken();
        assertTrue("accessToken didn't match expected "+accessToken+", actual "+actualAccessToken,
                accessToken.equals(actualAccessToken));
        
        int minExpiresIn = secondsFromNow - 5;
        Long actualExpiresIn = response.getExpiresIn();
        int maxExpiresIn = secondsFromNow + 5;
        assertTrue("expected expiresIn between " + minExpiresIn + " and " + maxExpiresIn + ", but got " 
                + actualExpiresIn, 
                null != actualExpiresIn && minExpiresIn <= actualExpiresIn && actualExpiresIn <= maxExpiresIn);

        Long actualExp = response.getExp();
        assertTrue("expected exp " + exp + ", actual " + actualExp,
                exp.equals(actualExp));
    }
}