/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.client;

import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

import elemental.dom.Element;

public class ExistingElementMapTest {

    @Test
    public void add_idAndElementAreReturnedByGetters() {
        ExistingElementMap map = new ExistingElementMap();
        Element element = Mockito.mock(Element.class);
        map.add(1, element);

        Assert.assertEquals(element, map.getElement(1));
        Assert.assertEquals(Integer.valueOf(1), map.getId(element));
    }

    @Test
    public void remove_idAndElementAreNotReturnedByGetters() {
        ExistingElementMap map = new ExistingElementMap();
        Element element = Mockito.mock(Element.class);
        map.add(1, element);

        map.remove(1);

        Assert.assertNull(map.getElement(1));
        Assert.assertNull(map.getId(element));
    }
}
