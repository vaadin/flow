/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.component;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import com.vaadin.flow.component.HasText.WhiteSpace;
import com.vaadin.flow.dom.Element;
import com.vaadin.flow.dom.ElementFactory;

public class HasTextTest {

    private HasText hasText = Mockito.mock(HasText.class);

    @Before
    public void setUp() {
        Element element = ElementFactory.createDiv();
        Mockito.when(hasText.getElement()).thenReturn(element);

        Mockito.doCallRealMethod().when(hasText).setWhiteSpace(Mockito.any());
        Mockito.doCallRealMethod().when(hasText).getWhiteSpace();
    }

    @Test
    public void setWhiteSpace_styleIsSet() {
        hasText.setWhiteSpace(WhiteSpace.NOWRAP);

        Assert.assertEquals("nowrap",
                hasText.getElement().getStyle().get("white-space"));
    }

    @Test
    public void getWhiteSpace_getStyleValue() {
        hasText.getElement().getStyle().set("white-space", "inherit");

        Assert.assertEquals(WhiteSpace.INHERIT, hasText.getWhiteSpace());
    }

    @Test
    public void getWhiteSpace_noStyleIsSet_normalIsReturned() {
        Assert.assertEquals(WhiteSpace.NORMAL, hasText.getWhiteSpace());
    }

    @Test
    public void getWhiteSpace_notStandardValue_nullIsReturned() {
        hasText.getElement().getStyle().set("white-space", "foo");

        Assert.assertEquals(null, hasText.getWhiteSpace());
    }

}
