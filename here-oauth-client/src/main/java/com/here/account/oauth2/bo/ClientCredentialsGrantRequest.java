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
package com.here.account.oauth2.bo;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * For grant_type=client_credentials.
 * 
 * @author kmccrack
 *
 */
public class ClientCredentialsGrantRequest extends AuthorizationRequest {
    
    public ClientCredentialsGrantRequest() {
        super("client_credentials");
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public String toJson() {
        return "{\"grantType\":\"" + getGrantType()
            + "\"}";
    }

    @Override
    public Map<String, List<String>> toFormParams() {
        Map<String, List<String>> formParams = new HashMap<String, List<String>>();
        addFormParam(formParams, "grant_type", getGrantType());
        return formParams;
    }

}
