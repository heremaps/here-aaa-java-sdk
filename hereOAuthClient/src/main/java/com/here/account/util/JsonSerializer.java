/*
 * Copyright 2016 HERE Global B.V.
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

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public class JsonSerializer {

    public static final String CHARSET_STRING = "UTF-8";
    public static final Charset CHARSET = Charset.forName(CHARSET_STRING);
    
    private static ObjectMapper objectMapper = new ObjectMapper();

    static {
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
    }
    
    public static Map<String, Object> toMap(InputStream jsonInputStream) throws IOException {
        return (HashMap<String, Object>) objectMapper.readValue(jsonInputStream, HashMap.class);
    }
    
    public static <T> T toPojo (InputStream jsonInputStream, Class<T> pojoClass) throws JsonParseException, JsonMappingException, IOException {
        return objectMapper.readValue(jsonInputStream, pojoClass);
    }
    
    public static String toJson(Map<String, Object> mapObject) throws JsonProcessingException {
        return objectMapper.writeValueAsString(mapObject);
    }

}
