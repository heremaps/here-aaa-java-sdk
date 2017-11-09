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

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * One of the OAuth2.0 
 * <a href="https://tools.ietf.org/html/rfc6749#section-1.3">Authorization Grant</a> Request 
 * types supported by HERE.
 * 
 * @author kmccrack
 *
 */
public abstract class AccessTokenRequest {
    
    /**
     * expiresIn; the parameter name for "expires in" when conveyed in a JSON body.
     */
    private static final String EXPIRES_IN_JSON = "expiresIn";
    
    /**
     * expires_in; the parameter name for "expires in" when conveyed in a form body.
     */
    private static final String EXPIRES_IN_FORM = "expires_in";
    
    protected static final String GRANT_TYPE_JSON = "grantType";
    protected static final String GRANT_TYPE_FORM = "grant_type";
    protected static final String SCOPE_JSON = "scope";
    protected static final String SCOPE_FORM = "scope";
    
    private final String grantType;

    /**
     * The optional lifetime in seconds of the access token returned by 
     * this request.
     * 
     * <p>
     * This property is a HERE extension to RFC6749 providing additional data.
     */
    private Long expiresIn;

    private String scope;
    
    protected AccessTokenRequest(String grantType) {
        this.grantType = grantType;
    }
    
    public String getGrantType() {
        return grantType;
    }
    
    /**
     * Optionally set the lifetime in seconds of the access token returned by 
     * this request.
     * Must be a positive number.  Ignored if greater than the maximum expiration 
     * for the client application.
     * Typically you can set this from 1 to 86400, the latter representing 24 
     * hours.
     * 
     * <p>
     * While the OAuth2.0 RFC doesn't list this as a request parameter, 
     * we add this so the client can request Access Token expirations within the 
     * allowable range.  See also the response parameter
     * <a href="https://tools.ietf.org/html/rfc6749#section-5.1">expires_in</a>.
     * 
     * <p>
     * This property is a HERE extension to RFC6749 providing additional data.
     * 
     * @param expiresIn desired lifetime in seconds of the access token
     * @return this
     * @see AccessTokenResponse#getExpiresIn()
     * @see #getExpiresIn()
     */
    public AccessTokenRequest setExpiresIn(Long expiresIn) {
        this.expiresIn = expiresIn;
        return this;
    }

    /**
     * Gets the expiresIn value, the desired lifetime in seconds of the access token 
     * returned by this request.
     * 
     * <p>
     * This property is a HERE extension to RFC6749 providing additional data.
     * 
     * @return the expiresIn value, the desired lifetime in seconds of the access token 
     *      returned by this request.
     */
    public Long getExpiresIn() {
        return this.expiresIn;
    }

    /**
     * Get the scope for the token.
     * The example value is "openid
     * sdp:GROUP-6bb1bfd9-8bdc-46c2-85cd-754068aa9497,
     * GROUP-84ba52de-f80b-4047-a024-33d81e6153df"
     * openid : Specifies the idToken is expected in the response
     * sdp:[List of groupId separated by ',']
     */
    public String getScope() {
        return this.scope;
    }

    /**
     * Get the scope for the token.
     * The example value is "openid
     * sdp:GROUP-6bb1bfd9-8bdc-46c2-85cd-754068aa9497,
     * GROUP-84ba52de-f80b-4047-a024-33d81e6153df"
     */
    public AccessTokenRequest setScope(String scope) {
        this.scope = scope;
        return this;
    }

    /**
     * Converts this request, into its JSON body representation.
     * 
     * @return the JSON body, for use with application/json bodies
     */
    public String toJson() {
        return "{\"" + GRANT_TYPE_JSON + "\":\"" + getGrantType() + "\""
            + (null != expiresIn ? ",\"" + EXPIRES_IN_JSON + "\":" + expiresIn : "")
            + ",\"" + SCOPE_JSON + "\":\"" + getScope() + "\"}";
    }

    /**
     * Converts this request, to its formParams Map representation.
     * 
     * @return the formParams, for use with application/x-www-form-urlencoded bodies.
     */
    public Map<String, List<String>> toFormParams() {
        Map<String, List<String>> formParams = new HashMap<String, List<String>>();
        addFormParam(formParams, GRANT_TYPE_FORM, getGrantType());
        addFormParam(formParams, EXPIRES_IN_FORM, getExpiresIn());
        addFormParam(formParams, SCOPE_FORM, getScope());
        return formParams;
    }

    /**
     * Adds the specified name and value to the form parameters.
     * If the value is non-null, the name and singleton-List of the value.toString() is 
     * added to the formParams Map.
     * 
     * @param formParams the formParams Map, for use with application/x-www-form-urlencoded bodies
     * @param name the name of the form parameter
     * @param value the value of the form parameter
     */
    protected final static void addFormParam(Map<String, List<String>> formParams, String name, Object value) {
        if (null != formParams && null != name && null != value) {
            formParams.put(name, Collections.singletonList(value.toString()));
        }
    }
}
