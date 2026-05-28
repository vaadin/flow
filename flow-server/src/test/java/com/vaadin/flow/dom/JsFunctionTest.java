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
package com.vaadin.flow.dom;

import java.util.List;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class JsFunctionTest {

    @Test
    void withArguments_alreadySet_throws() {
        JsFunction fn = JsFunction.of("return a;").withArguments("a");
        assertThrows(IllegalStateException.class, () -> fn.withArguments("b"));
    }

    @Test
    void withParameter_prependsLetAndAppendsCapture() {
        JsFunction fn = JsFunction.of("return target.id;")
                .withParameter("target", "value");
        assertEquals("let target=$0;return target.id;", fn.getBody());
        assertEquals(List.of("value"), fn.getCaptures());
    }

    @Test
    void withParameter_chained_indexesIncrement() {
        JsFunction fn = JsFunction.of("a + b").withParameter("a", 1)
                .withParameter("b", 2);
        assertEquals("let b=$1;let a=$0;a + b", fn.getBody());
        assertEquals(List.of(1, 2), fn.getCaptures());
    }

    @Test
    void withParameter_afterOfCaptures_indexesContinue() {
        JsFunction fn = JsFunction.of("$0 + b", "x").withParameter("b", 1);
        assertEquals("let b=$1;$0 + b", fn.getBody());
        assertEquals(List.of("x", 1), fn.getCaptures());
    }

    @Test
    void withParameter_duplicateName_throws() {
        JsFunction fn = JsFunction.of("x").withParameter("x", 1);
        assertThrows(IllegalArgumentException.class,
                () -> fn.withParameter("x", 2));
    }

    @Test
    void withParameter_collidesWithArgumentName_throws() {
        JsFunction fn = JsFunction.of("x").withArguments("x");
        assertThrows(IllegalArgumentException.class,
                () -> fn.withParameter("x", 1));
    }
}
