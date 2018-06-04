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

import org.junit.Assert;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

public class ReadUtilTest {

    @Test
    public void test_readUpTo16KBytes() throws IOException {
        String str = "The quick brown fox jumped over the lazy dog";
        InputStream inputStream = new ByteArrayInputStream(str.getBytes("UTF-8"));
        byte[] bytes = ReadUtil.readUpTo16KBytes(inputStream);
        Assert.assertEquals(str, new String(bytes));
    }
}
