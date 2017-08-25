package com.here.account.oauth2;

import org.junit.Assert;
import org.junit.Test;

public class AccessTokenResponseTest {

    @Test
    public void testIdTokenIsSetViaConstructor() {
        String expectedIdToken = "idToken";
        AccessTokenResponse response = new AccessTokenResponse("accessToken",
                "testType", 1200L, "testToken", expectedIdToken);

        Assert.assertEquals(response.getIdToken(), expectedIdToken);
    }
}
