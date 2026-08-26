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
package com.vaadin.flow.component.html;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class NativeTableColumnTest extends ComponentTest {
    // Actual test methods in super class

    @Override
    protected void addProperties() {
        addProperty("span", int.class, 1, 2, false, false);
    }

    @Test
    void defaultSpanIsOne() {
        NativeTableColumn col = (NativeTableColumn) getComponent();
        assertEquals(1, col.getSpan());
    }

    @Test
    void setSpan_writesAttribute() {
        NativeTableColumn col = (NativeTableColumn) getComponent();
        col.setSpan(3);
        assertEquals("3", col.getElement().getAttribute("span"));
        assertEquals(3, col.getSpan());
    }

    @Test
    void setSpan_rejectsNonPositive() {
        NativeTableColumn col = (NativeTableColumn) getComponent();
        assertThrows(IllegalArgumentException.class, () -> col.setSpan(0));
        assertThrows(IllegalArgumentException.class, () -> col.setSpan(-1));
    }

    @Test
    void resetSpan_removesAttribute() {
        NativeTableColumn col = (NativeTableColumn) getComponent();
        col.setSpan(4);
        col.resetSpan();
        assertNull(col.getElement().getAttribute("span"));
        assertEquals(1, col.getSpan());
    }

    @Test
    void spanConstructor() {
        NativeTableColumn col = new NativeTableColumn(5);
        assertEquals(5, col.getSpan());
    }
}
