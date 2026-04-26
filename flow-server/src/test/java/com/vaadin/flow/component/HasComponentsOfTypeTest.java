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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

class HasComponentsOfTypeTest {

    @Tag("breadcrumb-item")
    static class Item extends Component {
        Item(String id) {
            setId(id);
        }
    }

    @Tag("breadcrumb-trail")
    static class Trail extends Component implements HasComponentsOfType<Item> {
    }

    @Test
    void typedContainer_addGetIndexOf() {
        Trail trail = new Trail();
        Item a = new Item("a");
        Item b = new Item("b");

        // Compile-time check: add only accepts Item or subtypes.
        trail.add(a, b);

        assertEquals(2, trail.getComponentCount());
        // getComponentAt returns T (Item) without needing a cast at the call
        // site, which is the main point of the typed interface.
        Item first = trail.getComponentAt(0);
        assertSame(a, first);
        assertEquals(1, trail.indexOf(b));
    }
}
