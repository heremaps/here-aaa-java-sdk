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
package com.here.account.auth.provider.incubator;

import java.util.List;
import java.util.Map;

import com.here.account.auth.NoAuthorizer;
import com.here.account.http.HttpConstants.HttpMethods;
import com.here.account.http.HttpProvider.HttpRequestAuthorizer;
import com.here.account.oauth2.AccessTokenRequest;
import com.here.account.oauth2.ClientAuthorizationRequestProvider;
import com.here.account.util.Clock;

/**
 * An incubator class that may be removed in subsequent releases,
 * or refactored into the parent package.
 * 
 * <p>
 * Gets authorization Access Tokens from an identity access token file.
 * 
 * @deprecated subject to removal, or non-backwards-compatible changes
 * @author kmccrack
 */
public class RunAsIdAuthorizationProvider implements ClientAuthorizationRequestProvider {

    /**
     * The HERE Access Token URL.
     */
    private static final String RUN_AS_ID_TOKEN_ENDPOINT_URL = 
            "http://localhost:8001/token";
    
    private final String tokenEndpointUrl;
    
    public RunAsIdAuthorizationProvider() {
        this(RUN_AS_ID_TOKEN_ENDPOINT_URL);
    }

    public RunAsIdAuthorizationProvider(String tokenEndpointUrl) {
        this.tokenEndpointUrl = tokenEndpointUrl;
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public String getTokenEndpointUrl() {
        return tokenEndpointUrl;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public HttpRequestAuthorizer getClientAuthorizer() {
        return new NoAuthorizer();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public AccessTokenRequest getNewAccessTokenRequest() {
        return new AccessTokenRequest(null) {
            
            /**
             * HTTP GETs cannot have request bodies.
             * 
             * @return null, indicating no form params/request body
             */
            @Override
            public Map<String, List<String>> toFormParams() {
                return null;
            }
        };
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public HttpMethods getHttpMethod() {
        return HttpMethods.GET;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Clock getClock() {
        // not sure
        return null;
    }
    
}
