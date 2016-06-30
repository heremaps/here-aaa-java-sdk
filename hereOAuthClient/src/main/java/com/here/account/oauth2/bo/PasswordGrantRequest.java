package com.here.account.oauth2.bo;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * For grant_type=password.
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
