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
    void withParameter_appendsCaptureAndAliasesInBody() {
        JsFunction fn = JsFunction.of("doSomething(foo)").withParameter("foo",
                "value");

        assertEquals("let foo=$0;doSomething(foo)", fn.getBody());
        assertEquals(List.of("value"), fn.getCaptures());
        assertEquals(List.of(), fn.getArgumentNames());
    }

    @Test
    void withParameter_combinesWithPositionalCaptures() {
        JsFunction fn = JsFunction.of("doSomething($0, foo)", 42)
                .withParameter("foo", "bar");

        assertEquals("let foo=$1;doSomething($0, foo)", fn.getBody());
        assertEquals(List.of(42, "bar"), fn.getCaptures());
    }

    @Test
    void withParameter_returnsNewInstance_doesNotMutateOriginal() {
        JsFunction original = JsFunction.of("doSomething(foo)");
        JsFunction withParam = original.withParameter("foo", "value");

        assertEquals("doSomething(foo)", original.getBody());
        assertEquals(List.of(), original.getCaptures());
        assertEquals("let foo=$0;doSomething(foo)", withParam.getBody());
    }

    @Test
    void withParameter_duplicateName_throws() {
        JsFunction fn = JsFunction.of("body").withParameter("foo", 1);
        assertThrows(IllegalArgumentException.class,
                () -> fn.withParameter("foo", 2));
    }

    @Test
    void withParameter_invalidName_throws() {
        JsFunction fn = JsFunction.of("body");
        assertThrows(IllegalArgumentException.class,
                () -> fn.withParameter("$0", 1));
        assertThrows(IllegalArgumentException.class,
                () -> fn.withParameter("1foo", 1));
        assertThrows(IllegalArgumentException.class,
                () -> fn.withParameter("", 1));
        assertThrows(IllegalArgumentException.class,
                () -> fn.withParameter(null, 1));
    }

}
