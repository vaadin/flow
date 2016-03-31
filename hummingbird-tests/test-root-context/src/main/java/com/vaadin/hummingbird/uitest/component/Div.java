/*
 * Copyright 2000-2016 Vaadin Ltd.
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
package com.vaadin.hummingbird.uitest.component;

import com.vaadin.hummingbird.dom.ElementFactory;
import com.vaadin.ui.Component;

public class Div extends AbstractComponent implements HasSimpleAddComponent {
    public Div() {
        super(ElementFactory.createDiv());
    }

    public Div(Component... components) {
        this();
        for (Component component : components) {
            addComponent(component);
        }
    }

    public Div setText(String text) {
        getElement().setTextContent(text);
        return this;
    }

    @Override
    public Div addClass(String className) {
        return (Div) super.addClass(className);
    }

    @Override
    public Div setId(String id) {
        return (Div) super.setId(id);
    }

}
