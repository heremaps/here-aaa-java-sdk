package com.here.account.client;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import com.here.account.http.HttpException;
import com.here.account.http.HttpProvider;
import com.here.account.oauth2.AccessTokenException;
import com.here.account.oauth2.AccessTokenResponse;
import com.here.account.oauth2.ErrorResponse;
import com.here.account.oauth2.ResponseParsingException;
import com.here.account.util.JacksonSerializer;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Client2Test {

    private HttpProvider backwardCompatibleHttpProvider;
    private Client client;
    private HttpProvider.HttpResponse httpResponse;
    private JacksonSerializer serializer;
    private String accessToken;
    private String bodyString;
    private int statusCode;
    private HttpProvider.HttpRequest httpRequest;

    private void setUp200() {
        accessToken = "value";
        bodyString = "{\"access_token\":\"" + accessToken + "\"}";
        statusCode = 200;

        setupClient();
    }

    private void setUp403_fromProxy() {
        accessToken = "value";
        bodyString = "<html><head><title>Forbidden</title></head><body>403 Forbiddent</body></html>";
        statusCode = 403;

        setupClient();
    }

    private void setUpResponse() {
        final byte[] bodyBytes = bodyString.getBytes(StandardCharsets.UTF_8);
        final int contentLength = bodyBytes.length;

        if (overrideHeaders) {
            this.httpResponse = new HttpProvider.HttpResponse() {

                @Override
                public int getStatusCode() {
                    return statusCode;
                }

                @Override
                public long getContentLength() {
                    return contentLength;
                }

                @Override
                public InputStream getResponseBody() throws IOException {
                    return new ByteArrayInputStream(bodyBytes);
                }

                @Override
                public Map<String, List<String>> getHeaders() {
                    Map<String, List<String>> headers = new HashMap<String, List<String>>();
                    headers.put("Content-Type", Collections.singletonList("text/html"));
                    return headers;
                }
            };

        } else {
            this.httpResponse = new HttpProvider.HttpResponse() {

                @Override
                public int getStatusCode() {
                    return statusCode;
                }

                @Override
                public long getContentLength() {
                    return contentLength;
                }

                @Override
                public InputStream getResponseBody() throws IOException {
                    return new ByteArrayInputStream(bodyBytes);
                }
            };
        }

    }

    private boolean overrideHeaders = false;

    private void setUpProvider() {
        backwardCompatibleHttpProvider = new HttpProvider() {

            @Override
            public void close() throws IOException {

            }

            @Override
            public HttpRequest getRequest(HttpRequestAuthorizer httpRequestAuthorizer, String method, String url, String requestBodyJson) {
                return null;
            }

            @Override
            public HttpRequest getRequest(HttpRequestAuthorizer httpRequestAuthorizer, String method, String url, Map<String, List<String>> formParams) {
                return null;
            }

            @Override
            public HttpResponse execute(HttpRequest httpRequest) throws HttpException, IOException {
                return httpResponse;
            }
        };

    }

    private void setupClient() {
        setUpResponse();
        setUpProvider();
        serializer = new JacksonSerializer();

        client = Client.builder()
                // TODO: possibly add good defaults in the Client.Builder to avoid NullPointerExceptions
                .withHttpProvider(backwardCompatibleHttpProvider)
                .withSerializer(serializer)
                .build();
        httpRequest = new HttpProvider.HttpRequest() {

            @Override
            public void addAuthorizationHeader(String value) {
                throw new UnsupportedOperationException();
            }
        };

    }

    @Test
    public void test_client_httpResponse_backwardsCompatible() {
        setUp200();
        AccessTokenResponse accessTokenResponse = client.sendMessage(httpRequest, AccessTokenResponse.class, ErrorResponse.class,
                (statusCode, errorResponse) -> {
                    return new AccessTokenException(statusCode, errorResponse);
                });
        assertTrue("accessTokenResponse was null", null != accessTokenResponse);
        String actualAccessToken = accessTokenResponse.getAccessToken();
        assertTrue("expected access_token " + accessToken + ", actual " + actualAccessToken,
                accessToken.equals(actualAccessToken));
    }

    @Test
    public void test_client_httpResponse_backwardsCompatible_403fromProxy_noContentType() {
        // the server did not tell us it wasn't JSON, so we send it to our JSON serializer,
        // and get back an appropriate ResponseParsingException.
        setUp403_fromProxy();
        try {
            AccessTokenResponse accessTokenResponse = client.sendMessage(httpRequest, AccessTokenResponse.class, ErrorResponse.class,
                    (statusCode, errorResponse) -> {
                        return new AccessTokenException(statusCode, errorResponse);
                    });
            assertTrue("accessTokenResponse was null", null != accessTokenResponse);
            String actualAccessToken = accessTokenResponse.getAccessToken();
            assertTrue("expected access_token " + accessToken + ", actual " + actualAccessToken,
                    accessToken.equals(actualAccessToken));
        } catch (ResponseParsingException e) {
            String message = e.getMessage();
            String expectedContains = "<";
            assertTrue("message " + message + " was expected to contain " + expectedContains + ", but didn't",
                    null != message && message.contains(expectedContains));
        }
    }

    @Test
    public void test_client_httpResponse_backwardsCompatible_403fromProxy_withContentType() {
        // the server told us it was Content-Type: text/html, which is unrecognized,
        // so we should just throw an Exception containing first up to 1KB of body.
        this.overrideHeaders = true;
        setUp403_fromProxy();
        try {
            AccessTokenResponse accessTokenResponse = client.sendMessage(httpRequest, AccessTokenResponse.class, ErrorResponse.class,
                    (statusCode, errorResponse) -> {
                        return new AccessTokenException(statusCode, errorResponse);
                    });
            fail("test case should have thrown exception, but didn't");
        } catch (AccessTokenException e) {
            int actualStatusCode = e.getStatusCode();
            assertTrue("statusCode was expected " + statusCode + ", actual " + actualStatusCode, statusCode == actualStatusCode);
            String message = e.getMessage();
            assertTrue("message " + message + " was expected to contain " + bodyString + ", but didn't",
                    null != message && message.contains(bodyString));
        }
    }


}
