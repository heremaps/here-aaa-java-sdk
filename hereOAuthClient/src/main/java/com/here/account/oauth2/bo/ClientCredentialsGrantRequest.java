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
