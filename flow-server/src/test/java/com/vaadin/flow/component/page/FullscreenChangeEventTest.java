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
package com.vaadin.flow.component.page;

import org.junit.jupiter.api.Test;

import com.vaadin.tests.util.MockUI;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FullscreenChangeEventTest {

    @Test
    public void eventReturnsConstructorValues() {
        Page page = new Page(new MockUI());

        FullscreenChangeEvent enterEvent = new FullscreenChangeEvent(page,
                true);
        assertTrue(enterEvent.isFullscreen());
        assertEquals(page, enterEvent.getSource());

        FullscreenChangeEvent exitEvent = new FullscreenChangeEvent(page,
                false);
        assertFalse(exitEvent.isFullscreen());
    }
}
