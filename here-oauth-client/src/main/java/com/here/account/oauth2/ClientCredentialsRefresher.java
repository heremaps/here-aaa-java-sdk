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

import com.here.account.bo.AuthenticationHttpException;
import com.here.account.bo.AuthenticationRuntimeException;
import com.here.account.http.HttpException;
import com.here.account.oauth2.bo.AccessTokenResponse;
import com.here.account.oauth2.bo.ClientCredentialsGrantRequest;
import com.here.account.util.RefreshableResponseProvider.ResponseRefresher;

public class ClientCredentialsRefresher implements ResponseRefresher<AccessTokenResponse> {
    
    private AuthorizationObtainer signIn;
    
    public ClientCredentialsRefresher(AuthorizationObtainer signIn) {
        this.signIn = signIn;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public AccessTokenResponse refresh(AccessTokenResponse previous) {
        try {
            return signIn.postToken(new ClientCredentialsGrantRequest());
        } catch (IOException | AuthenticationHttpException | HttpException e) {
            throw new AuthenticationRuntimeException("trouble refresh: " + e, e);
        }
    }
  
}
