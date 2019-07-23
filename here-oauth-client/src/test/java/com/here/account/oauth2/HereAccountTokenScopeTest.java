package com.here.account.oauth2;

import com.here.account.auth.provider.FromDefaultHereCredentialsPropertiesFile;
import com.here.account.http.HttpProvider;
import org.junit.Test;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

import static org.junit.Assert.*;

public class HereAccountTokenScopeTest extends AbstractCredentialTezt {
    private static final String url = "https://qa.account.api.here.com/oauth2/token";
    private static final String clientId = "vFVk3Wt83DU6NLVkagYc";
    private static final String accessKeyId = "hjZ6-B8lMYOWS0mo25QnTA";
    private static final String accessKeySecret = "Gxo_S_XNUaBH7xGV_Z1BS_59mgUJNcd2EF4SirNjxEOyULuvHUb0VwSe9XGkWAvPHsBt3sOlkIi7NbMAXtwMlg";
    private static final String TEST_PROJECT = "hrn:here-dev:authorization::test-realm-qa-inviteonlyfalse:project/test-project-qa-1";

    @Test
    public void testGetProjectScopedToken() throws Exception {
        HttpProvider httpProvider = getHttpProvider();
        ClientAuthorizationRequestProvider clientAuthorizationRequestProvider = getClientAuthorizationRequestProvider();
        TokenEndpoint tokenEndpoint = HereAccount.getTokenEndpoint(httpProvider, clientAuthorizationRequestProvider);

        AccessTokenResponse accessTokenResponse = tokenEndpoint.requestToken(new ClientCredentialsGrantRequest());
        assertNotNull("accessTokenResponse was null", accessTokenResponse);
        assertEquals("scope in request and response should be the same", TEST_PROJECT, accessTokenResponse.getScope());
    }

    private ClientAuthorizationRequestProvider getClientAuthorizationRequestProvider() throws Exception {
        String prefix = UUID.randomUUID().toString();
        File file = File.createTempFile(prefix, null);
        file.deleteOnExit();

        byte[] bytes = ("here.token.endpoint.url="+url+"\n"
                + "here.client.id="+clientId+"\n"
                + "here.access.key.id="+accessKeyId+"\n"
                + "here.access.key.secret="+accessKeySecret+"\n"
                + "here.token.scope="+TEST_PROJECT)
                .getBytes(StandardCharsets.UTF_8);
        try (FileOutputStream outputStream = new FileOutputStream(file)) {
            outputStream.write(bytes);
            outputStream.flush();
        }

        return new FromDefaultHereCredentialsPropertiesFile(file);
    }
}
