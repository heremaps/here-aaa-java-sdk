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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.here.account.olp.OlpHttpMessage;
import com.here.account.util.JacksonSerializer;

/**
 * One of the OAuth2.0 
 * <a href="https://tools.ietf.org/html/rfc6749#section-1.3">Authorization Grant</a> Request 
 * types supported by HERE.
 * 
 * @author kmccrack
 *
 */
public abstract class AccessTokenRequest implements OlpHttpMessage {
    
    /**
     * expiresIn; the parameter name for "expires in" when conveyed in a JSON body.
     * @deprecated use {@link JacksonSerializer} instead
     */
    private static final String EXPIRES_IN_JSON = "expiresIn";
    
    /**
     * expires_in; the parameter name for "expires in" when conveyed in a form body.
     */
    protected static final String EXPIRES_IN_FORM = "expires_in";
    
    /**
     * grantType; the parameter name for "grant type" when conveyed in a JSON body.
     * @deprecated use {@link JacksonSerializer} instead
     */
    protected static final String GRANT_TYPE_JSON = "grantType";
    protected static final String GRANT_TYPE_FORM = "grant_type";
    
    /**
     * scope; the parameter name for "scope" when conveyed in a JSON body.
     * @deprecated use {@link JacksonSerializer} instead
     */
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
    private List<String> resource;
    private transient Map<String, String> additionalHeaders = null;
    private transient String correlationId = null;
    
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
     * Get the scope for the token request.  See also 
     * <a href="http://openid.net/specs/openid-connect-core-1_0.html#ScopeClaims">
     * Requesting Claims using Scope Values</a>.
     * 
     * <p>
     * The example values are "openid
     * sdp:GROUP-6bb1bfd9-8bdc-46c2-85cd-754068aa9497,
     * GROUP-84ba52de-f80b-4047-a024-33d81e6153df"
     * openid : Specifies the idToken is expected in the response
     * sdp:[List of groupId separated by ',']
     *  or
     * hrn:here:authorization::rlm0000:project/my-project-0000
     * A projectHRN for a Project-scoped token
     * 
     * @return the scope
     */
    public String getScope() {
        return this.scope;
    }

    /**
     * Set the scope for the token request.  See also 
     * <a href="http://openid.net/specs/openid-connect-core-1_0.html#ScopeClaims">
     * Requesting Claims using Scope Values</a>.
     * 
     * <p>
     * The example values are "openid
     * sdp:GROUP-6bb1bfd9-8bdc-46c2-85cd-754068aa9497,
     * GROUP-84ba52de-f80b-4047-a024-33d81e6153df".
     *  or
     * hrn:here:authorization::rlm0000:project/my-project-0000
     * A projectHRN for a Project-scoped token
     * 
     * @param scope the scope to set
     * @return  this
     */
    public AccessTokenRequest setScope(String scope) {
        this.scope = scope;
        return this;
    }

    /**
     * Get any additional headers that will be added to the token request.
     *
     * @return the additional headers
     */
    public Map<String, String> getAdditionalHeaders() { return additionalHeaders; }

    public AccessTokenRequest setAdditionalHeaders(Map<String, String> additionalHeaders) {
        this.additionalHeaders = additionalHeaders;
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getCorrelationId() {
        return this.correlationId;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public AccessTokenRequest setCorrelationId(String correlationId) {
        this.correlationId = correlationId;
        return this;
    }

    /**
     * Converts this request, into its JSON body representation.
     * 
     * @return the JSON body, for use with application/json bodies
     * @deprecated use {@link JacksonSerializer} instead
     */
    @Deprecated
    public String toJson() {
        return "{\"" + GRANT_TYPE_JSON + "\":\"" + getGrantType() + "\""
            + (null != expiresIn ? ",\"" + EXPIRES_IN_JSON + "\":" + expiresIn : "")
            + ",\"" + SCOPE_JSON + "\":\"" + getScope() + "\"}";
    }

    /**
     * Get the RFC 8707 resource indicator URIs for this token request.
     *
     * @return the list of resource URIs, or null if not set
     */
    public List<String> getResource() {
        return resource;
    }

    /**
     * Set the RFC 8707 resource indicator URIs for this token request.
     * Each value must be an absolute URI per RFC 8707 §2. Query components
     * SHOULD NOT be included; fragments MUST NOT be included.
     * Sent as repeated {@code resource=} form parameters in the
     * {@code application/x-www-form-urlencoded} POST body — not as URL query parameters.
     *
     * <p>No client-side URI validation is performed. Validation is the authorization
     * server's responsibility per RFC 8707 §2; client-side checks would duplicate
     * server logic and risk divergence.
     *
     * @param resource list of absolute resource URIs
     * @return this
     * @see <a href="https://www.rfc-editor.org/rfc/rfc8707">RFC 8707</a>
     */
    public AccessTokenRequest setResource(List<String> resource) {
        this.resource = resource;
        return this;
    }

    public Map<String, List<String>> toFormParams() {
        Map<String, List<String>> formParams = new HashMap<String, List<String>>();
        addFormParam(formParams, GRANT_TYPE_FORM, getGrantType());
        addFormParam(formParams, EXPIRES_IN_FORM, getExpiresIn());
        addFormParam(formParams, SCOPE_FORM, getScope());
        addFormParams(formParams, "resource", resource);
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

    /**
     * Adds a multi-value form parameter entry to the given map.
     * A defensive copy of {@code values} is stored.
     * No-op if any of {@code formParams}, {@code name}, or {@code values} is null or empty.
     *
     * @param formParams the map to populate; must not be null
     * @param name       the parameter name; must not be null
     * @param values     the parameter values; must not be null or empty
     */
    protected final static void addFormParams(Map<String, List<String>> formParams, String name, List<String> values) {
        if (null != formParams && null != name && null != values && !values.isEmpty()) {
            formParams.put(name, new ArrayList<>(values));
        }
    }
}
