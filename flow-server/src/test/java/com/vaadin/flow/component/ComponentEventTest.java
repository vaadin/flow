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

import com.vaadin.flow.component.ComponentTest.TestComponent;
import com.vaadin.tests.util.MockUI;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ComponentEventTest {

    @Test
    void getUI_sourceAttached_returnsUI() {
        MockUI ui = new MockUI();
        TestComponent source = new TestComponent();
        ui.add(source);

        ComponentEvent<TestComponent> event = new ComponentEvent<>(source,
                false);

        assertSame(ui, event.getUI());
    }

    @Test
    void getUI_sourceIsUI_returnsSource() {
        MockUI ui = new MockUI();

        ComponentEvent<UI> event = new ComponentEvent<>(ui, false);

        assertSame(ui, event.getUI());
    }

    @Test
    void getUI_sourceDetached_throwsIllegalStateException() {
        TestComponent source = new TestComponent();

        ComponentEvent<TestComponent> event = new ComponentEvent<>(source,
                false);

        IllegalStateException exception = assertThrows(
                IllegalStateException.class, event::getUI);
        assertNotNull(exception.getMessage());
        assertEquals(true, exception.getMessage().contains("not")
                && exception.getMessage().contains("attached"));
    }
}
