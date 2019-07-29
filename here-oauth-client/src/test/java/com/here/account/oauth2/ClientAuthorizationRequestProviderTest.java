package com.here.account.oauth2;

import static org.junit.Assert.assertTrue;

import com.here.account.http.HttpConstants;
import com.here.account.http.HttpProvider;
import com.here.account.util.Clock;
import org.junit.Test;

import java.util.List;
import java.util.Map;

public class ClientAuthorizationRequestProviderTest {

    ClientAuthorizationRequestProvider myProvider;

    @Test
    public void test_backward_compatible_interface() {
        // DO NOT add any new method implementations here!

        this.myProvider = new ClientAuthorizationRequestProvider() {
            @Override
            public String getTokenEndpointUrl() {
                return "tokenEndpointUrl";
            }

            @Override
            public HttpProvider.HttpRequestAuthorizer getClientAuthorizer() {
                return new HttpProvider.HttpRequestAuthorizer() {
                    @Override
                    public void authorize(HttpProvider.HttpRequest httpRequest, String method, String url, Map<String, List<String>> formParams) {
                        // no-op
                    }
                };
            }

            @Override
            public AccessTokenRequest getNewAccessTokenRequest() {
                return new ClientCredentialsGrantRequest();
            }

            @Override
            public HttpConstants.HttpMethods getHttpMethod() {
                return HttpConstants.HttpMethods.POST;
            }

            @Override
            public Clock getClock() {
                return Clock.SYSTEM;
            }
        };

        assertTrue("myProvider was null", null != myProvider);
    }

    @Test
    public void test_backward_compatible_getScope() {
        // DO NOT add any new method implementations here!

        test_backward_compatible_interface();

        String scope = myProvider.getScope();
        assertTrue("scope was expected null", null == scope);
    }



}
