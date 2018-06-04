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
package com.here.account.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Map;

import com.fasterxml.jackson.core.JsonProcessingException;

/**
 * A Serializer that uses Jackson to serialize and deserialize JSON.
 *
 * @author kmccrack
 */
public class JacksonSerializer implements Serializer {

    public JacksonSerializer() {
    }
    
    /**
     * {@inheritDoc}
     */
    public Map<String, Object> jsonToMap(InputStream jsonInputStream) {
        try {
            return JsonSerializer.toMap(jsonInputStream);
        } catch (IOException e) {
            throw new RuntimeException("trouble deserializing json: " + e, e);
        }
    }

    @Override
    public <T> T jsonToPojo(InputStream jsonInputStream, Class<T> pojoClass) {
        try {
            return JsonSerializer.toPojo(jsonInputStream, pojoClass);
        } catch (IOException e) {
            throw new RuntimeException("trouble deserializing json: " + e, e);
        }
    }

    @Override
    public String objectToJson(Object object) {
        try {
            return JsonSerializer.objectToJson(object);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("trouble serializing json: " + e, e);
        }
    }

    @Override
    public void writeObjectToJson(OutputStream outputStream, Object object) {
        try {
            JsonSerializer.writeObjectToJson(outputStream, object);
        } catch (IOException e) {
            throw new RuntimeException("trouble serializing json: " + e, e);
        }
    }
    

}
