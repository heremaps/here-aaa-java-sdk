package com.here.account.oauth2;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import com.here.account.util.JsonSerializer;

/**
 * Temporary class to deal with converting lower-upper camel case keys json,
 * to underscore-lower delimited keys.
 * 
 * @author kmccrack
 *
 */
public class LowerUpperCamelCaseToUnderscoreConverter {

    /**
     * This method will effectively become a no-op once HEREACCT-3452 is implemented and released.
     * At which time, the inefficiency can be removed.
     * 
     * @param lowerUpperInputStream the input stream for a lower-upper camel case json top-level keys
     * @return the input stream for an underscore-separated json top-level keys
     * @throws IOException
     */
    public static final InputStream convertRootKeysToUnderscores(InputStream lowerUpperInputStream) throws IOException {
        Map<String, Object> map = JsonSerializer.toMap(lowerUpperInputStream);
        Map<String, Object> result = new HashMap<String, Object>();
        for (Entry<String, Object> entry : map.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();
            result.put(convertToUnderscores(key), value);
        }
        String json = JsonSerializer.toJson(result);
        byte[] utf8bytes = json.getBytes(JsonSerializer.CHARSET);
        return new ByteArrayInputStream(utf8bytes);
    }

    private static final int A_CHAR = (int) 'A';
    private static final int Z_CHAR = (int) 'Z';
    private static final int DIFF_A_a = ((int) 'A') - ((int) 'a');
    
    /**
     * Any capital letter in 'key' is replaced with an underscore character, 
     * plus the lower-case equivalent character.
     * 
     * @param key
     * @return
     */
    private static String convertToUnderscores(String key) {
        StringBuilder buf = new StringBuilder();
        for (int i = 0; i < key.length(); i++) {
            char a = key.charAt(i);
            if (a >= A_CHAR && a <= Z_CHAR) {
                buf.append('_');
                a -= DIFF_A_a;
            }
            buf.append(a);
        }
        return buf.toString();
    }
}
