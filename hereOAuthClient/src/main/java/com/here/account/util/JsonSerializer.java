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
