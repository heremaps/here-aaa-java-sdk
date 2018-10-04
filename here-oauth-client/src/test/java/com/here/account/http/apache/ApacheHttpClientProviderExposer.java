package com.here.account.http.apache;

import com.here.account.http.HttpProvider;
import org.apache.http.client.methods.HttpRequestBase;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class ApacheHttpClientProviderExposer {

    public static HttpRequestBase getHttpRequestBase(HttpProvider.HttpRequest httpRequest) {

        try {
            Method method = httpRequest.getClass().getDeclaredMethod("getHttpRequestBase");
            method.setAccessible(true);
            return (HttpRequestBase) method.invoke(httpRequest, null);
        } catch (Exception e) {
            throw new RuntimeException("trouble: " + e, e);
        }
    }
}
