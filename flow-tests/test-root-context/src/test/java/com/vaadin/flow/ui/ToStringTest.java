/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.ui;

import java.util.Collection;

import org.junit.Assert;
import org.junit.Test;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.uitest.servlet.ViewClassLocator;

public class ToStringTest {
    @Test
    public void testViewsElementsStringable() throws Exception {
        Collection<Class<? extends Component>> viewClasses = new ViewClassLocator(
                getClass().getClassLoader()).getAllViewClasses();
        for (Class<? extends Component> viewClass : viewClasses) {
            Component view = viewClass.newInstance();
            String string = view.getElement().toString();
            Assert.assertNotNull(string);
            Assert.assertNotEquals("", string);
        }
    }

}
