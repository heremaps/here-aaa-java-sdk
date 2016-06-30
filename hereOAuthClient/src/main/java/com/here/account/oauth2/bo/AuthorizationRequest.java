package com.here.account.oauth2.bo;

import java.util.Collections;
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
public abstract class AuthorizationRequest {

    static final String GRANT_TYPE = "grantType";
    
    private final String grantType;
    
    protected AuthorizationRequest(String grantType) {
        this.grantType = grantType;
    }
    
    public String getGrantType() {
        return grantType;
    }
    
    /**
     * Converts this request, into its JSON body representation.
     * 
     * @return the JSON body, for use with application/json bodies
     */
    public abstract String toJson();
    
    /**
     * Converts this request, to its formParams Map representation.
     * 
     * @return the formParams, for use with application/x-www-form-urlencoded bodies.
     */
    public abstract Map<String, List<String>> toFormParams();

    /**
     * If the value is non-null, the name and singleton-List of the value.toString() is 
     * added to the formParams Map.
     * 
     * @param formParams the formParams Map, for use with application/x-www-form-urlencoded bodies
     * @param name the name of the form parameter
     * @param value the value of the form parameter
     */
    protected final void addFormParam(Map<String, List<String>> formParams, String name, Object value) {
        if (null != value) {
            formParams.put(name, Collections.singletonList(value.toString()));
        }
    }

}
