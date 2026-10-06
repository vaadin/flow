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
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import com.vaadin.flow.internal.streams.ByteRangeUtil.ByteRange;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ByteRangeUtilTest {

    @ParameterizedTest
    @CsvSource(delimiter = '|', nullValues = "null", value = {
            "bytes=2-5     | 2    | 5", "bytes=7-      | 7    | 9",
            "bytes=-3      | 7    | 9", "bytes=-99     | 0    | 9",
            "bytes=5-99    | 5    | 9", "bytes=10-     | 10   | 9",
            "bytes=-0      | 10   | 9", "null          | null | null",
            "bytes=-       | null | null", "bytes=5-2     | null | null",
            "bytes=0-1,4-5 | null | null", "items=2-5     | null | null" })
    void parseRange_resolvesAgainstContentLength(String header, Long start,
            Long end) {
        Optional<ByteRange> expected = start == null ? Optional.empty()
                : Optional.of(new ByteRange(start, end));

        assertEquals(expected, ByteRangeUtil.parseRange(header, 10));
    }

    @Test
    void limit_readsAtMostLength() throws IOException {
        InputStream limited = ByteRangeUtil.limit(new ByteArrayInputStream(
                "abcdefghij".getBytes(StandardCharsets.UTF_8)), 4);

        assertEquals("abcd",
                new String(limited.readAllBytes(), StandardCharsets.UTF_8));
        assertEquals(-1, limited.read());
    }
}
