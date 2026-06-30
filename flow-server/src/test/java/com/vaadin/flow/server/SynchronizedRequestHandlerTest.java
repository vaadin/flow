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
package com.vaadin.flow.server;

import java.io.StringReader;
import java.util.Arrays;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

public class SynchronizedRequestHandlerTest {

    @Test
    public void getRequestBody_underLimit_readsBody() throws Exception {
        String body = repeat('a', 1000);
        assertEquals(body, SynchronizedRequestHandler
                .getRequestBody(new StringReader(body), 1000));
    }

    @Test
    public void getRequestBody_overLimit_throws() {
        String body = repeat('a', 1001);
        assertThrows(RequestBodyTooLargeException.class,
                () -> SynchronizedRequestHandler
                        .getRequestBody(new StringReader(body), 1000));
    }

    @Test
    public void getRequestBody_negativeLimit_readsWithoutLimit()
            throws Exception {
        String body = repeat('a', 200_000);
        assertEquals(body, SynchronizedRequestHandler
                .getRequestBody(new StringReader(body), -1));
    }

    private static String repeat(char c, int count) {
        char[] chars = new char[count];
        Arrays.fill(chars, c);
        return new String(chars);
    }
}
