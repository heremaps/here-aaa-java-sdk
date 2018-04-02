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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * A jackson-based JSON serializer and deserializer.
 * 
 * @author kmccrack
 *
 */
public class JsonSerializer {

    /**
     * The name of the UTF-8 {@link #CHARSET}.
     */
    public static final String CHARSET_STRING = "UTF-8";

    /**
     * Constant for the loaded UTF-8 Charset.
     */
    public static final Charset CHARSET = Charset.forName(CHARSET_STRING);
    
    private static ObjectMapper objectMapper = new ObjectMapper();

    static {
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
    }
    
    /**
     * Converts the input JSON InputStream, to a Map&lt;String, Object&gt;.
     * 
     * @param jsonInputStream the input stream to the JSON object
     * @return its Map representation
     * @throws IOException if trouble deserializing
     */
    public static Map<String, Object> toMap(InputStream jsonInputStream) throws IOException {
        return (HashMap<String, Object>) objectMapper.readValue(jsonInputStream, HashMap.class);
    }
    
    /**
     * Converts the input JSON InputStream, to a POJO of the class specified as pojoClass.
     * 
     * @param <T> the type of the POJO
     * @param jsonInputStream the input stream to the JSON object
     * @param pojoClass the class to deserialize into
     * @return the instance of the pojoClass with member variables populated
     * @throws JsonParseException if trouble parsing
     * @throws JsonMappingException if trouble mapping
     * @throws IOException if trouble deserializing
     */
    public static <T> T toPojo (InputStream jsonInputStream, Class<T> pojoClass) throws JsonParseException, JsonMappingException, IOException {
        return objectMapper.readValue(jsonInputStream, pojoClass);
    }
    
    /**
     * Converts the input mapObject to its JSON String representation.
     * 
     * @param mapObject the json's Map representation
     * @return the JSON String representation of the input.
     * @throws JsonProcessingException if an exception from the jackson serializer
     */
    public static String toJson(Map<String, Object> mapObject) throws JsonProcessingException {
        return objectMapper.writeValueAsString(mapObject);
    }
    
    /**
     * Converts the input POJO object to its JSON string.
     * 
     * @param object the object to serialize into a JSON string.
     * @return the JSON string representation of the object.
     * @throws JsonProcessingException if there's trouble serializing object 
     *      to a JSON string.
     */
    public static String objectToJson(Object object) throws JsonProcessingException {
        return objectMapper.writeValueAsString(object);
    }
    
    /**
     * Writes the object to the specified outputStream as JSON.
     * 
     * @param outputStream the OutputStream to which to write
     * @param object the object to write to the stream
     * @throws JsonGenerationException if trouble serializing
     * @throws JsonMappingException if trouble serializing
     * @throws IOException if I/O trouble writing to the stream
     */
    static void writeObjectToJson(OutputStream outputStream, Object object) throws JsonGenerationException, JsonMappingException, IOException {
        objectMapper.writeValue(outputStream, object);
    }

}
