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

import com.here.account.http.HttpProvider;

/**
 * A {@code ClientCredentialsProvider} identifies a token endpoint and provides
 * a mechanism to inject client credentials into access token requests.
 */
public interface ClientCredentialsProvider {
    
    /**
     * Gets the url of the token endpoint for which this provider's client
     * credentials are valid.
     * 
     * @return the url of the token endpoint
     */
    String getTokenEndpointUrl();
    
    /**
     * Gets the {@code HttpRequestAuthorizer} that will inject
     * client credentials into access token requests.
     * 
     * @return the client credentials authorizer
     */
    HttpProvider.HttpRequestAuthorizer getClientAuthorizer();
}
