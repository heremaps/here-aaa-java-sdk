/*
 * Copyright (c) 2025 HERE Europe B.V.
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

import java.util.List;
import java.util.Map;

/**
 * An {@link AccessTokenRequest} for grant_type=client_credentials using
 * JWT client assertion authentication per RFC 7523 Section 2.2 and OAuth 2.1.
 *
 * <p>
 * The token request includes:
 * <ul>
 *   <li>{@code grant_type} = {@code client_credentials}</li>
 *   <li>{@code client_assertion_type} = {@code urn:ietf:params:oauth:client-assertion-type:jwt-bearer}</li>
 *   <li>{@code client_assertion} = a signed JWT (RS256)</li>
 * </ul>
 *
 * @see <a href="https://www.rfc-editor.org/rfc/rfc7523#section-2.2">RFC 7523 §2.2</a>
 * @see <a href="https://datatracker.ietf.org/doc/html/draft-ietf-oauth-v2-1-13">OAuth 2.1 Draft</a>
 */
public class ClientAssertionCredentialsGrantRequest extends AccessTokenRequest {

    public static final String CLIENT_CREDENTIALS_GRANT_TYPE = "client_credentials";

    /**
     * The client_assertion_type value for JWT Bearer assertions per RFC 7523.
     */
    public static final String CLIENT_ASSERTION_TYPE_JWT_BEARER =
            "urn:ietf:params:oauth:client-assertion-type:jwt-bearer";

    protected static final String CLIENT_ASSERTION_TYPE_FORM = "client_assertion_type";
    protected static final String CLIENT_ASSERTION_FORM = "client_assertion";

    private final String clientAssertion;

    /**
     * Construct a new client assertion grant request.
     *
     * @param clientAssertion the signed JWT client assertion
     */
    public ClientAssertionCredentialsGrantRequest(String clientAssertion) {
        super(CLIENT_CREDENTIALS_GRANT_TYPE);
        if (clientAssertion == null || clientAssertion.isEmpty()) {
            throw new IllegalArgumentException("clientAssertion is required");
        }
        this.clientAssertion = clientAssertion;
    }

    /**
     * Gets the client assertion JWT.
     *
     * @return the signed JWT assertion
     */
    public String getClientAssertion() {
        return clientAssertion;
    }

    /**
     * {@inheritDoc}
     *
     * Adds client_assertion_type and client_assertion to the form parameters.
     */
    @Override
    public Map<String, List<String>> toFormParams() {
        Map<String, List<String>> formParams = super.toFormParams();
        addFormParam(formParams, CLIENT_ASSERTION_TYPE_FORM, CLIENT_ASSERTION_TYPE_JWT_BEARER);
        addFormParam(formParams, CLIENT_ASSERTION_FORM, clientAssertion);
        return formParams;
    }
}
