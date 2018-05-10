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

import java.lang.reflect.Field;

public class OAuth1SignerExposer {

    /**
     * Using reflection, get the accessKeyId from the oAuth1Signer.
     * For test purposes only, we break the abstraction barrier.
     *
     * @param oAuth1Signer the OAuth1Signer whose accessKeyId to get
     * @return the accessKeyId
     * @throws NoSuchFieldException if the field is no longer defined
     * @throws IllegalAccessException if access to the field's value is not permitted
     */
    public static String getAccessKeyId(OAuth1Signer oAuth1Signer) throws NoSuchFieldException, IllegalAccessException {
        return getStringField(oAuth1Signer,"consumerKey");
    }

    protected static String getStringField(OAuth1Signer oAuth1Signer, String fieldName) throws NoSuchFieldException, IllegalAccessException {
        Field field = OAuth1Signer.class.getDeclaredField(fieldName);
        field.setAccessible(true);
        return (String) field.get(oAuth1Signer);
    }

    /**
     * Using reflection, get the accessKeySecret from the oAuth1Signer.
     * For test purposes only, we break the abstraction barrier.
     *
     * @param oAuth1Signer the OAuth1Signer whose accessKeySecret to get
     * @return the accessKeySecret
     * @throws NoSuchFieldException if the field is no longer defined
     * @throws IllegalAccessException if access to the field's value is not permitted
     */
    public static String getAccessKeySecret(OAuth1Signer oAuth1Signer) throws NoSuchFieldException, IllegalAccessException {
        return getStringField(oAuth1Signer,"consumerSecret");

    }
}
