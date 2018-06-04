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
package com.here.account.identity.bo;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.here.account.oauth2.AccessTokenRequest;

public class IdentityTokenRequest extends AccessTokenRequest {
    
    public static final String IDENTITY_GRANT_TYPE = "identity";
    
    private String runAsId;
    private String namespace;
    private String runAsIdName;
    private String podName;
    private String podUid;
    
    public IdentityTokenRequest() {
        super(IDENTITY_GRANT_TYPE);
    }

    public String getRunAsId() {
        return runAsId;
    }

    public IdentityTokenRequest withRunAsId(String runAsId) {
        this.runAsId = runAsId;
        return this;
    }

    public String getNamespace() {
        return namespace;
    }

    public IdentityTokenRequest withNamespace(String namespace) {
        this.namespace = namespace;
        return this;
    }

    public String getRunAsIdName() {
        return runAsIdName;
    }

    public IdentityTokenRequest withRunAsIdName(String runAsIdName) {
        this.runAsIdName = runAsIdName;
        return this;
    }

    public String getPodName() {
        return podName;
    }

    public IdentityTokenRequest withPodName(String podName) {
        this.podName = podName;
        return this;
    }
    
    public String getPodUid() {
        return podUid;
    }
    
    public IdentityTokenRequest withPodUid(String podUid) {
        this.podUid = podUid;
        return this;
    }

    
    private static final String RUN_AS_ID_FORM = "runAsId";
    private static final String NAMESPACE_FORM = "namespace";
    private static final String RUN_AS_ID_NAME_FORM = "runAsIdName";
    private static final String POD_NAME_FORM = "podName";
    private static final String POD_UID_FORM = "podUid";

    /**
     * Converts this request, to its formParams Map representation.
     * 
     * @return the formParams, for use with application/x-www-form-urlencoded bodies.
     */
    @Override
    public Map<String, List<String>> toFormParams() {
        Map<String, List<String>> formParams = super.toFormParams();
        addFormParam(formParams, RUN_AS_ID_FORM, getRunAsId());
        addFormParam(formParams, NAMESPACE_FORM, getNamespace());
        addFormParam(formParams, RUN_AS_ID_NAME_FORM, getRunAsIdName());
        addFormParam(formParams, POD_NAME_FORM, getPodName());
        addFormParam(formParams, POD_UID_FORM, getPodUid());
        return formParams;
    }
    
    /**
     * Converts the formParams generated from an instance of IdentityTokenRequest, 
     * back into the IdentityTokenRequest with proper properties set.
     * 
     * @param formParams the formParams representing an IdentityTokenRequest
     * @return the resulting IdentityTokenRequest
     */
    public static IdentityTokenRequest fromFormParams(Map<String, List<String>> formParams) {
        IdentityTokenRequest identityTokenRequest = new IdentityTokenRequest()
                .withRunAsId(getSingleFormValue(formParams, RUN_AS_ID_FORM))
                .withNamespace(getSingleFormValue(formParams, NAMESPACE_FORM))
                .withRunAsIdName(getSingleFormValue(formParams, RUN_AS_ID_NAME_FORM))
                .withPodName(getSingleFormValue(formParams, POD_NAME_FORM))
                .withPodUid(getSingleFormValue(formParams, POD_UID_FORM));
        String expiresInString = getSingleFormValue(formParams, EXPIRES_IN_FORM);
        if (null != expiresInString) {
            Long expiresIn = Long.parseLong(expiresInString);
            identityTokenRequest.setExpiresIn(expiresIn);
        }
        identityTokenRequest.setScope(getSingleFormValue(formParams, SCOPE_FORM));
        return identityTokenRequest;
    }
    
    /**
     * If available, gets the first value matching the formParamKey from formParams.
     * 
     * @param formParams the form parameters
     * @param formParamKey the key 
     * @return the first form value for the key
     */
    protected static String getSingleFormValue(
            Map<String, List<String>> formParams, String formParamKey) {
        List<String> values = formParams.get(formParamKey);
        if (null != values && values.size() > 0) {
            return values.get(0);
        }
        return null;
    }

    
}
