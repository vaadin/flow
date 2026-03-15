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
package com.vaadin.flow.component;

import org.junit.jupiter.api.Test;

import com.vaadin.flow.component.HasText.WhiteSpace;

import static org.junit.jupiter.api.Assertions.assertEquals;

class WhiteSpaceTest {

    @Test
    public void toString_styleValueIsReturned() {
        assertEquals("nowrap", WhiteSpace.NOWRAP.toString());
        assertEquals("pre-line", WhiteSpace.PRE_LINE.toString());
    }

    @Test
    public void forString_enumIsReturned() {
        assertEquals(WhiteSpace.NORMAL, WhiteSpace.forString("normal"));
        assertEquals(WhiteSpace.PRE_WRAP, WhiteSpace.forString("pre-wrap"));
    }
}
