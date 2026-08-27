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
package com.vaadin.flow.devloop.daemon;

import java.util.List;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * The daemon emits its own JSON, so escaping is the one thing between a
 * diagnostic and an unparseable reply. A compiler message routinely carries
 * quotes, newlines and Windows paths.
 */
class JsonTest {

    @Test
    void escape_specialCharacters_areEscaped() {
        assertEquals("say \\\"hi\\\"", Json.escape("say \"hi\""));
        assertEquals("C:\\\\app\\\\Foo.java", Json.escape("C:\\app\\Foo.java"));
        assertEquals("one\\ntwo\\rthree\\tfour",
                Json.escape("one\ntwo\rthree\tfour"));
    }

    @Test
    void escape_controlCharacter_becomesUnicodeEscape() {
        assertEquals("a\\u0000b", Json.escape("a\u0000b"));
    }

    @Test
    void escape_null_isEmptyString() {
        assertEquals("", Json.escape(null));
    }

    @Test
    void strings_quotesAndEscapesEveryItem() {
        assertEquals("[\"a\",\"b\\\"c\"]", Json.strings(List.of("a", "b\"c")));
        assertEquals("[]", Json.strings(List.of()));
    }
}
