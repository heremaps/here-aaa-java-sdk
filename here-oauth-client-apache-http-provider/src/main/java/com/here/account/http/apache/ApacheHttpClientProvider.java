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
package com.here.account.http.apache;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.http.HttpEntity;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpEntityEnclosingRequestBase;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpHead;
import org.apache.http.client.methods.HttpOptions;
import org.apache.http.client.methods.HttpPatch;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.client.methods.HttpTrace;
import org.apache.http.entity.BasicHttpEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.HttpContext;

import com.here.account.http.HttpConstants;
import com.here.account.http.HttpException;
import com.here.account.http.HttpProvider;

/**
 * An {@link HttpProvider} that uses Apache HttpClient as the underlying implementation.
 * See <a href="https://hc.apache.org/httpcomponents-client-ga/">Apache HTTP Components</a> 
 * for more information, specifically the HttpClient project.
 * 
 * <p>
 * If you just want the default 5000 ms for both connection timeout and request timeout, 
 * just use the following example code:
 * <pre>
 * {@code
       HttpProvider httpProvider = ApacheHttpClientProvider.builder().build();
       // use httpProvider such as with HereAccessTokenProviders...
 * }
 * </pre>
 * 
 * @author kmccrack
 *
 */
public class ApacheHttpClientProvider implements HttpProvider {

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private RequestConfig.Builder apacheConfigBuilder;
        private CloseableHttpClient httpClient;
        private boolean doCloseHttpClient = true;

        private Builder() {
            apacheConfigBuilder = RequestConfig.custom();
            setConnectionTimeoutInMs(HttpConstants.DEFAULT_CONNECTION_TIMEOUT_IN_MS);
            setRequestTimeoutInMs(HttpConstants.DEFAULT_REQUEST_TIMEOUT_IN_MS);
        }

        public Builder setHttpClient(final CloseableHttpClient httpClient) {
            this.httpClient = httpClient;
            return this;
        }

        public Builder setDoCloseHttpClient(final boolean doCloseHttpClient) {
            this.doCloseHttpClient = doCloseHttpClient;
            return this;
        }

        public Builder setRequestTimeoutInMs(int requestTimeoutInMs) {
            this.apacheConfigBuilder.setSocketTimeout(requestTimeoutInMs);
            return this;
        }

        public Builder setConnectionTimeoutInMs(int connectionTimeoutInMs) {
            this.apacheConfigBuilder.setConnectTimeout(connectionTimeoutInMs)
                    .setConnectionRequestTimeout(connectionTimeoutInMs);
            return this;
        }

        /**
         * Build using builders, builders, and more builders.
         * 
         * @return the built HttpProvider implementation for Apache httpclient.
         */
        public HttpProvider build() {

            CloseableHttpClient client = this.httpClient != null ? this.httpClient :
            // uses PoolingHttpClientConnectionManager by default
                    HttpClientBuilder.create().setDefaultRequestConfig(apacheConfigBuilder.build()).build();

            return new ApacheHttpClientProvider(client, this.doCloseHttpClient);

        }
    }

    private static class ApacheHttpClientRequest implements HttpRequest {
        
        private final HttpRequestBase httpRequestBase;
        
        private ApacheHttpClientRequest(HttpRequestBase httpRequestBase) {
            this.httpRequestBase = httpRequestBase;
        }
        
        private HttpRequestBase getHttpRequestBase() {
            return httpRequestBase;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void addAuthorizationHeader(String value) {
            httpRequestBase.addHeader(HttpConstants.AUTHORIZATION_HEADER, value);
        }
    }
    
    private static class ApacheHttpClientResponse implements HttpResponse {
        
        private final org.apache.http.HttpResponse apacheHttpResponse;
        
        private ApacheHttpClientResponse(org.apache.http.HttpResponse apacheHttpResponse) {
            this.apacheHttpResponse = apacheHttpResponse;
        }
        
        /**
         * {@inheritDoc}
         */
        @Override
        public int getStatusCode() {
            return apacheHttpResponse.getStatusLine().getStatusCode();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public InputStream getResponseBody() throws IOException {
            HttpEntity httpEntity = apacheHttpResponse.getEntity();
            if (null != httpEntity) {
                return httpEntity.getContent();
            }
            return null;
        }
        
        /**
         * {@inheritDoc}
         */
        @Override
        public long getContentLength() {
            HttpEntity httpEntity = apacheHttpResponse.getEntity();
            if (null != httpEntity) {
                return httpEntity.getContentLength();
            }
            return 0L;
        }

    }

    private HttpRequestBase getRequestNoAuth(String method, String url) {
        URI uri;
        try {
            uri = new URI(url);
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException("malformed URL: " + e, e);
        }
        HttpRequestBase apacheRequest = null;
        if (method.equals(HttpGet.METHOD_NAME)) {
            apacheRequest = new HttpGet(uri);
        } else if (method.equals(HttpPost.METHOD_NAME)) {
            apacheRequest = new HttpPost(uri);
        } else if (method.equals(HttpPut.METHOD_NAME)) {
            apacheRequest = new HttpPut(uri);
        } else if (method.equals(HttpDelete.METHOD_NAME)) {
            apacheRequest = new HttpDelete(uri);
        } else if (method.equals(HttpHead.METHOD_NAME)) {
            apacheRequest = new HttpHead(uri);
        } else if (method.equals(HttpOptions.METHOD_NAME)) {
            apacheRequest = new HttpOptions(uri);
        } else if (method.equals(HttpTrace.METHOD_NAME)) {
            apacheRequest = new HttpTrace(uri);
        } else if (method.equals(HttpPatch.METHOD_NAME)) {
            apacheRequest = new HttpPatch(uri);
        } else {
            throw new IllegalArgumentException("no support for request method=" + method);
        }
            
        /*
        // headers support
        String contentType = null;
        FluentCaseInsensitiveStringsMap headers = request.getHeaders();
        if (null != headers) {
            for (Entry<String, List<String>> entry : headers.entrySet()) {
                String name = entry.getKey();
                List<String> valueList = entry.getValue();
                if (null != name && null != valueList && valueList.size() > 0) {
                    for (String value : valueList) {
                        if (null == contentType && name.equals(AbstractHttpRequest.CONTENT_TYPE)) {
                            contentType = value;
                        }
                        apacheRequest.addHeader(name, value);
                    }
                }
            }
        }
        */
        
        return apacheRequest;
    }
    
    private void addApacheRequestEntity(HttpRequestBase apacheRequest, 
            String method,
            String requestBodyJson,
            Map<String, List<String>> formParams) {
        HttpEntityEnclosingRequestBase apacheRequestSupportsEntity = 
                apacheRequest instanceof HttpEntityEnclosingRequestBase 
                ? (HttpEntityEnclosingRequestBase) apacheRequest 
                        : null;

        // body support
        if (null != formParams && formParams.size() > 0) {
            if (null == apacheRequestSupportsEntity) {
                throw new IllegalArgumentException("no formParams permitted for method "+method);
            }
            // form parameters support
            // application/x-www-form-urlencoded only
            apacheRequestSupportsEntity.addHeader(HttpConstants.CONTENT_TYPE, HttpConstants.CONTENT_TYPE_FORM_URLENCODED);
            List<NameValuePair> parameters = new ArrayList<NameValuePair>();
            for (Entry<String, List<String>> entry : formParams.entrySet()) {
                String key = entry.getKey();
                List<String> valueList = entry.getValue();
                if (null != key && null != valueList && valueList.size() > 0) {
                    String value = valueList.get(0);
                    if (null != value) {
                        NameValuePair nameValuePair = new BasicNameValuePair(key, value);
                        parameters.add(nameValuePair);
                    }
                }
            }
            HttpEntity entity = new UrlEncodedFormEntity(parameters, HttpConstants.ENCODING_CHARSET);
            apacheRequestSupportsEntity.setEntity(entity);
        } else if (null != requestBodyJson) {
            if (null == apacheRequestSupportsEntity) {
                throw new IllegalArgumentException("no JSON request body permitted for method "+method);
            }
            // JSON body support
            apacheRequestSupportsEntity.addHeader(HttpConstants.CONTENT_TYPE, HttpConstants.CONTENT_TYPE_JSON);
            byte[] bodyBytes = requestBodyJson.getBytes(HttpConstants.ENCODING_CHARSET);
            if (null != bodyBytes) {
                BasicHttpEntity entity = new BasicHttpEntity();
                entity.setContent(new ByteArrayInputStream(bodyBytes));
                entity.setContentLength(bodyBytes.length);
                apacheRequestSupportsEntity.setEntity(entity);
            }
        }

    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public HttpRequest getRequest(HttpRequestAuthorizer httpRequestAuthorizer, String method, String url,
            String requestBodyJson) {
        HttpRequestBase apacheRequest = 
                /*String method, String url*/
                getRequestNoAuth(method, url);
        
        ApacheHttpClientRequest request = new ApacheHttpClientRequest(apacheRequest);
        
        // OAuth1
        // NOTE: because this example uses application/json, not forms, our request bodies are 
        // never part of the OAuth1 Authorization header.
        Map<String, List<String>> formParams = null;
        httpRequestAuthorizer.authorize(request, method, url, formParams);

        addApacheRequestEntity(apacheRequest, method, requestBodyJson, null);
        
        return request;
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public HttpRequest getRequest(HttpRequestAuthorizer httpRequestAuthorizer, String method, String url,
            Map<String, List<String>> formParams) {
        HttpRequestBase apacheRequest = 
                /*String method, String url*/
                getRequestNoAuth(method, url);
        
        ApacheHttpClientRequest request = new ApacheHttpClientRequest(apacheRequest);
        
        // OAuth1
        // with application/x-www-form-urlencoded bodies, 
        // the request body is supposed to impact the signature.
        httpRequestAuthorizer.authorize(request, method, url, formParams);

        addApacheRequestEntity(apacheRequest, method, null, formParams);
        
        return request;
    }

    private final CloseableHttpClient httpClient;
    private final boolean doCloseHttpClient;

    private ApacheHttpClientProvider(CloseableHttpClient httpClient, boolean doCloseHttpClient) {
        this.httpClient = httpClient;
        this.doCloseHttpClient = doCloseHttpClient;
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public void close() throws IOException {
        if (doCloseHttpClient && null != httpClient) {
            httpClient.close();
        }
    }

    @Override
    public HttpResponse execute(HttpRequest httpRequest) throws HttpException, IOException {
        if (!(httpRequest instanceof ApacheHttpClientRequest)) {
            throw new IllegalArgumentException("httpRequest is not of expected type; use "
                    +getClass()+".getRequest(..) to get a request of the expected type");
        }
        HttpRequestBase httpRequestBase = ((ApacheHttpClientRequest) httpRequest).getHttpRequestBase();
        
        // we are stateless
        HttpContext httpContext = null;
        
        try {
            // blocking
            org.apache.http.HttpResponse apacheHttpResponse = httpClient.execute(httpRequestBase, httpContext);
            
            return new ApacheHttpClientResponse(apacheHttpResponse);
        } catch (ClientProtocolException e) {
            throw new HttpException("trouble: " + e, e);
        }
    }

}