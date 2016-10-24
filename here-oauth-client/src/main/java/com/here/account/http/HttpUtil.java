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
package com.here.account.http;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Generic HTTP utility class.
 * 
 * @author kmccrack
 *
 */
public class HttpUtil {

    /**
     * Adds the specified name and value to the form parameters.
     * If the value is non-null, the name and singleton-List of the value.toString() is 
     * added to the formParams Map.
     * 
     * @param formParams the formParams Map, for use with application/x-www-form-urlencoded bodies
     * @param name the name of the form parameter
     * @param value the value of the form parameter
     */
    public static void addFormParam(Map<String, List<String>> formParams, String name, Object value) {
        if (null != formParams && null != name && null != value) {
            formParams.put(name, Collections.singletonList(value.toString()));
        }
    }

}
