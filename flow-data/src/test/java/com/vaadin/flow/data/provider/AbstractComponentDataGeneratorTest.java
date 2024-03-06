/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.data.provider;

import org.junit.Test;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Text;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.dom.Element;
import com.vaadin.flow.dom.ElementFactory;

import elemental.json.JsonObject;

public class AbstractComponentDataGeneratorTest {

    private static class TestComponentDataGenerator
            extends AbstractComponentDataGenerator<String> {

        @Override
        public void generateData(String item, JsonObject jsonObject) {
        }

        @Override
        protected Element getContainer() {
            Element element = ElementFactory.createAnchor();
            UI ui = new UI();
            ui.getElement().appendChild(element);
            return element;
        }

        @Override
        protected Component createComponent(String item) {
            return null;
        }

        @Override
        protected String getItemKey(String item) {
            return null;
        }

    }

    @Test(expected = IllegalStateException.class)
    public void registerRenderedComponent_containerHasUi_componentHasDifferentUi_throws() {
        TestComponentDataGenerator generator = new TestComponentDataGenerator();

        Component component = new Text("bar");
        UI ui = new UI();
        ui.add(component);
        generator.registerRenderedComponent("foo", component);
    }

}
