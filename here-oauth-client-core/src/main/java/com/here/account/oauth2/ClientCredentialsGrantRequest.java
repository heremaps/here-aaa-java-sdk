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

/**
 * An {@link AccessTokenRequest} for grant_type=client_credentials.
 * 
 * @author kmccrack
 *
 */
public class ClientCredentialsGrantRequest extends AccessTokenRequest {
    
    public static final String CLIENT_CREDENTIALS_GRANT_TYPE = "client_credentials";
    
    public ClientCredentialsGrantRequest() {
        super(CLIENT_CREDENTIALS_GRANT_TYPE);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ClientCredentialsGrantRequest setExpiresIn(Long expiresIn) {
        super.setExpiresIn(expiresIn);
        return this;
    }
    
}
