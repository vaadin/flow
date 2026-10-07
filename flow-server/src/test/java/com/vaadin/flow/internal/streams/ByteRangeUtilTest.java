/*
 * Copyright 2000-2026 Vaadin Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.vaadin.flow.internal.streams;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ByteRangeUtilTest {

    @Test
    void limit_readsAtMostLength() throws IOException {
        InputStream limited = ByteRangeUtil.limit(new ByteArrayInputStream(
                "abcdefghij".getBytes(StandardCharsets.UTF_8)), 4);

        assertEquals("abcd",
                new String(limited.readAllBytes(), StandardCharsets.UTF_8));
        assertEquals(-1, limited.read());
        assertEquals(0, limited.read(new byte[4], 0, 0));
    }
}
