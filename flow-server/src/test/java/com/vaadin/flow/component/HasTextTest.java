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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import com.vaadin.flow.component.HasText.WhiteSpace;
import com.vaadin.flow.dom.Element;
import com.vaadin.flow.dom.ElementFactory;
import com.vaadin.flow.dom.Style;

import static org.junit.jupiter.api.Assertions.assertEquals;

class HasTextTest {

    private HasText hasText = Mockito.mock(HasText.class);

    @BeforeEach
    public void setUp() {
        Element element = ElementFactory.createDiv();
        Mockito.when(hasText.getElement()).thenReturn(element);

        Mockito.doCallRealMethod().when(hasText).setWhiteSpace(Mockito.any());
        Mockito.doCallRealMethod().when(hasText).getWhiteSpace();
    }

    @Test
    public void setWhiteSpace_styleIsSet() {
        hasText.setWhiteSpace(WhiteSpace.NOWRAP);

        assertEquals("nowrap",
                hasText.getElement().getStyle().get("white-space"));
    }

    @Test
    public void getWhiteSpace_getStyleValue() {
        hasText.getElement().getStyle().setWhiteSpace(Style.WhiteSpace.INHERIT);
        assertEquals(WhiteSpace.INHERIT, hasText.getWhiteSpace());
    }

    @Test
    public void getWhiteSpace_noStyleIsSet_normalIsReturned() {
        assertEquals(WhiteSpace.NORMAL, hasText.getWhiteSpace());
    }

    @Test
    public void getWhiteSpace_notStandardValue_nullIsReturned() {
        hasText.getElement().getStyle().set("white-space", "foo");

        assertEquals(null, hasText.getWhiteSpace());
    }

}
