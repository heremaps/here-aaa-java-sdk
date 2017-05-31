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
package com.here.account.util;

import java.nio.charset.Charset;

/**
 * Utility class for constants used by the HERE oauth client.
 * 
 * @author kmccrack
 *
 */
public class OAuthConstants {
    
    /**
     * We commonly use "UTF-8" charset, this String is its name.
     */
    public static final String UTF_8_STRING = "UTF-8";
    
    /**
     * This is the constant for the "UTF-8" Charset already loaded.
     */
    public static final Charset UTF_8_CHARSET = Charset.forName(UTF_8_STRING);
}
