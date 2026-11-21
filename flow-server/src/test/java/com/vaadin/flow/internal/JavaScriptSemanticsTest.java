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
package com.vaadin.flow.internal;

import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.MissingNode;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JavaScriptSemanticsTest {

    @Test
    void javaValues() {
        assertFalse(JavaScriptSemantics.isTrueish(null));
        assertTrue(JavaScriptSemantics.isTrueish(Boolean.TRUE));
        assertFalse(JavaScriptSemantics.isTrueish(Boolean.FALSE));
        assertTrue(JavaScriptSemantics.isTrueish("false"));
        assertFalse(JavaScriptSemantics.isTrueish(""));
        assertTrue(JavaScriptSemantics.isTrueish(0.5));
        assertTrue(JavaScriptSemantics.isTrueish(-1));
        assertFalse(JavaScriptSemantics.isTrueish(0));
        assertFalse(JavaScriptSemantics.isTrueish(-0.0));
        assertFalse(JavaScriptSemantics.isTrueish(Double.NaN));
        assertThrows(IllegalStateException.class,
                () -> JavaScriptSemantics.isTrueish(new Object()));
    }

    @Test
    void jsonValues() {
        assertFalse(isTrueish("null"));
        assertTrue(isTrueish("true"));
        assertFalse(isTrueish("false"));
        assertTrue(isTrueish("\"Enter\""));
        assertTrue(isTrueish("\"false\""));
        assertFalse(isTrueish("\"\""));
        assertTrue(isTrueish("1"));
        assertTrue(isTrueish("0.5"));
        assertFalse(isTrueish("0"));
        assertFalse(isTrueish("-0.0"));
        assertTrue(isTrueish("{}"));
        assertTrue(isTrueish("[]"));
        assertFalse(JavaScriptSemantics.isTrueish(MissingNode.getInstance()));
    }

    private static boolean isTrueish(String json) {
        JsonNode node = JacksonUtils.getMapper().readTree(json);
        return JavaScriptSemantics.isTrueish(node);
    }
}
