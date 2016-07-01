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
 * For grant_type=password.
 * There is a danger we may have to remove this class as unsupported.
 * 
 * @author kmccrack
 *
 */
public class PasswordGrantRequest extends AuthorizationRequest {
    
    private String email;
    private String password;
    
    public PasswordGrantRequest() {
        super("password");
    }
    
    public PasswordGrantRequest setEmail(String email) {
        this.email = email;
        return this;
    }
    
    public PasswordGrantRequest setPassword(String password) {
        this.password = password;
        return this;
    }
    
    public String getEmail() {
        return email;
    }
    
    public String getPassword() {
        return password;
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public String toJson() {
        return "{\"grantType\":\"" + getGrantType()
            + "\",\"email\":\"" + getEmail()
            + "\",\"password\":\"" + getPassword() + "\"}";
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public Map<String, List<String>> toFormParams() {
        Map<String, List<String>> formParams = new HashMap<String, List<String>>();
        addFormParam(formParams, "grant_type", getGrantType());
        // TODO: this should be OAuth2.0-compliant "username" input
        addFormParam(formParams, "email", getEmail());
        addFormParam(formParams, "password", getPassword());
        return formParams;
    }

}
