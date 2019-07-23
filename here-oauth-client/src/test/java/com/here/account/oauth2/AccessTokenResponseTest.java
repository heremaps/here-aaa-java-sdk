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

import org.junit.Assert;
import org.junit.Test;

public class AccessTokenResponseTest {

    @Test
    public void testIdTokenIsSetViaConstructor() {
        String expectedAccessToken = "testAccessToken";
        String expectedTokenType = "testType";
        long expectedExpiresIn = 1200L;
        String expectedRefreshToken = "testRefreshToken";
        String expectedIdToken = "idToken";
        String expectedScope = "scope123";
        AccessTokenResponse response = new AccessTokenResponse(expectedAccessToken,
                expectedTokenType, expectedExpiresIn, expectedRefreshToken, expectedIdToken, expectedScope);

        Assert.assertEquals(expectedAccessToken, response.getAccessToken());
        Assert.assertEquals(expectedTokenType, response.getTokenType());
        Assert.assertEquals((long)expectedExpiresIn, (long)response.getExpiresIn());
        Assert.assertEquals(expectedRefreshToken, response.getRefreshToken());
        Assert.assertEquals(expectedIdToken, response.getIdToken());
        Assert.assertEquals(expectedScope, response.getScope());
    }

    @Test
    public void testGetSetCorrelationId() {
        String expectedCorrelationId = "testCorrelationId";
        AccessTokenResponse response = new AccessTokenResponse();
        response.setCorrelationId(expectedCorrelationId);
        Assert.assertEquals(expectedCorrelationId, response.getCorrelationId());
    }
}
