/*
 * Copyright 2000-2025 Vaadin Ltd.
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

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class HasPlaceholderTest {

    @Tag(Tag.DIV)
    private static class TestComponent extends Component
            implements HasPlaceholder {

    }

    @Test
    public void withoutPlaceholderComponent_getPlaceholderReturnsNull() {
        TestComponent component = new TestComponent();

        assertNull(component.getPlaceholder());
    }

    @Test
    public void withNullPlaceholder_getPlaceholderReturnsEmptyString() {
        TestComponent component = new TestComponent();
        component.setPlaceholder(null);
        assertEquals("", component.getPlaceholder());
    }

    @Test
    public void withEmptyPlaceholder_getPlaceholderReturnsEmptyString() {
        TestComponent component = new TestComponent();
        component.setPlaceholder("");
        assertEquals("", component.getPlaceholder());
    }

    @Test
    public void setPlaceholder() {
        TestComponent component = new TestComponent();
        component.setPlaceholder("test Placeholder");

        assertEquals("test Placeholder", component.getPlaceholder());
    }

}
