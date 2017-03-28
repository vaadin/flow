/*
 * Copyright 2015-2017 The original authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.vaadin.spring.internal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.After;
import org.junit.Test;
import org.mockito.Mockito;

import com.vaadin.ui.UI;

public class UUIDTest {

    @Test
    public void testSameIdEquals() {
        assertEquals(new UIID(1), new UIID(1));
    }

    @Test
    public void testDifferentIdNotEquals() {
        assertNotEquals(new UIID(1), new UIID(2));
    }

    @Test
    public void testNullNotEquals() {
        assertNotEquals(new UIID(1), null);
    }

    @Test
    public void testDifferentClassNotEquals() {
        assertNotEquals(new UIID(1), new Integer(1));
    }

    @Test
    public void testUIConstructor() {
        final int expected_id = 123;
        UI ui = mock(UI.class);
        when(ui.getUIId()).thenReturn(expected_id);
        UIID uiid = new UIID(ui);
        verify(ui, atLeast(1)).getUIId();
        assertEquals(new UIID(expected_id), uiid);
    }

    @After
    public void validate() {
        Mockito.validateMockitoUsage();
    }
}
