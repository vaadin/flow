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

import java.io.Serializable;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ToStringBuilderTest {

    private static class Base {
    }

    private static class Sub extends Base {
    }

    @Test
    void toString_namedBareAndOptionalValues() {
        assertEquals("Base[a=1, b=null, d=2, plain]",
                ToStringBuilder.of(new Base()).add("a", 1).add("b", null)
                        .addIfNotNull("c", null).addIfNotNull("d", 2)
                        .addValue("plain").toString());
        assertEquals("Base[]", ToStringBuilder.of(new Base()).toString());
    }

    @Test
    void toString_usesRuntimeClassName() {
        assertEquals("Sub[]", ToStringBuilder.of(new Sub()).toString());
    }

    @Test
    void toString_anonymousClass_usesNamedSupertype() {
        assertEquals("Base[]", ToStringBuilder.of(new Base() {
        }).toString());
        assertEquals("Serializable[]", ToStringBuilder.of(new Serializable() {
        }).toString());
    }
}
