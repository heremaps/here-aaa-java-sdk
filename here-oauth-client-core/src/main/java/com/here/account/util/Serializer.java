package com.here.account.util;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.Map;

public interface Serializer {
    
    /**
     * Reads from the input jsonInputStream containing bytes from a JSON stream, 
     * and returns the corresponding Map&lt;String, Object&gt;.
     * 
     * @param jsonInputStream the input stream
     * @return the corresponding deserialized Map&lt;String, Object&gt;
     */
    Map<String, Object> jsonToMap(InputStream jsonInputStream);
    
    <T> T jsonToPojo (InputStream jsonInputStream, Class<T> pojoClass);
    
    String objectToJson(Object object);
    
    void writeObjectToJson(OutputStream outputStream, Object object);

}
