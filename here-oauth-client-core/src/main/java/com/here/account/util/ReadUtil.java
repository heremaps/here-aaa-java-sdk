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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class ReadUtil {

    /**
     * 16 Kilobytes.
     */
    private final static int MAX_BYTES_TO_READ = 1024*16;
    
    public static byte[] readUpTo16KBytes(InputStream inputStream) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        byte[] buf = new byte[4096];
        int numRead;
        int totalRead = 0;
        while (totalRead < MAX_BYTES_TO_READ && (numRead = inputStream.read(buf)) > 0) {
            baos.write(buf, 0, numRead);
            totalRead += numRead;
        }
        return baos.toByteArray();
    }
}
