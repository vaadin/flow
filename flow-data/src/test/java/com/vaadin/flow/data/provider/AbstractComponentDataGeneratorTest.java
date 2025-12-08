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
package com.vaadin.flow.data.provider;

import org.junit.Test;
import tools.jackson.databind.node.ObjectNode;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Text;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.dom.Element;
import com.vaadin.flow.dom.ElementFactory;

public class AbstractComponentDataGeneratorTest {

    private static class TestComponentDataGenerator
            extends AbstractComponentDataGenerator<String> {

        @Override
        public void generateData(String item, ObjectNode jsonObject) {
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
