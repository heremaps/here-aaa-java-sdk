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
package com.here.account.auth;

import java.util.List;
import java.util.Map;

import com.here.account.http.HttpProvider;
import com.here.account.http.HttpProvider.HttpRequest;

/**
 * Use this class, for use cases where you want no Authorization header 
 * on your requests to the service.
 * 
 * @author kmccrack
 */
public class NoAuthorizer implements HttpProvider.HttpRequestAuthorizer {

    /**
     * Does nothing, as no Authorization header is required when using 
     * instances of this class.
     * 
     * <p>
     * {@inheritDoc}
     */
    @Override
    public void authorize(HttpRequest httpRequest, String method, String url, Map<String, List<String>> formParams) {
        // nothing to do
        // no Authorization header
    }

}
