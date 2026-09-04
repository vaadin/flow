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
import java.util.Optional;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * The daemon emits its own JSON, so escaping is the one thing between a
 * diagnostic and an unparseable reply. A compiler message routinely carries
 * quotes, newlines and Windows paths.
 * <p>
 * It also reads one string out of Flow's {@code flow-build-info.json}, where
 * the same Windows path arrives escaped and has to survive the round trip.
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

    /** A trimmed-down flow-build-info.json, in the shape Flow writes it. */
    private static final String BUILD_INFO = """
            {
              "productionMode" : false,
              "npmFolder" : "C:\\\\Projects\\\\flow\\\\app",
              "frontend.hotdeploy" : true,
              "frontendFolder" : "C:\\\\Projects\\\\flow\\\\app\\\\src\\\\main\\\\frontend",
              "build.folder" : "target"
            }
            """;

    @Test
    void stringField_windowsPath_isUnescaped() {
        assertEquals(
                Optional.of("C:\\Projects\\flow\\app\\src\\main\\frontend"),
                Json.stringField(BUILD_INFO, "frontendFolder"));
    }

    @Test
    void stringField_missingKey_isEmpty() {
        assertEquals(Optional.empty(),
                Json.stringField(BUILD_INFO, "notThere"));
    }

    @Test
    void stringField_nonStringValue_isEmpty() {
        // A boolean is "not a string", which is the caller's signal to fall
        // back rather than to fail.
        assertEquals(Optional.empty(),
                Json.stringField(BUILD_INFO, "frontend.hotdeploy"));
    }

    @Test
    void stringField_keyIsMatchedWhole() {
        // "frontend" is a prefix of "frontendFolder"; matching on the prefix
        // would hand back the wrong directory rather than nothing.
        assertEquals(Optional.empty(),
                Json.stringField(BUILD_INFO, "frontend"));
    }

    @Test
    void stringField_truncatedFile_isEmpty() {
        // A concurrent Maven run can be caught mid-write; half a path is worse
        // than no path.
        assertEquals(Optional.empty(), Json.stringField(
                "{\"frontendFolder\" : \"C:\\\\App", "frontendFolder"));
    }

    @Test
    void stringField_escapeSequences_areDecoded() {
        assertEquals(Optional.of("a\"b/c\td\u00e9"),
                Json.stringField("{\"k\":\"a\\\"b\\/c\\td\\u00e9\"}", "k"));
    }

    @Test
    void stringField_nullInput_isEmpty() {
        assertEquals(Optional.empty(), Json.stringField(null, "k"));
    }
}
