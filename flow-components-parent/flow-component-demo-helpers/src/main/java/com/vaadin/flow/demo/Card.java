/*
 * Copyright 2000-2018 Vaadin Ltd.
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
package com.vaadin.flow.demo;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.dependency.StyleSheet;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.dom.Element;
import com.vaadin.flow.dom.ElementFactory;

/**
 * Card to hold components for DemoViews
 *
 * @author Vaadin Ltd
 * @since 1.0
 */
@StyleSheet("src/css/component-card.css")
public class Card extends Div {

    /**
     * Card constructor that set wanted styles.
     */
    public Card() {
        getElement().setAttribute("class", "component-card");
    }

    @Override
    public void add(Component... components) {
        assert components != null;
        for (Component component : components) {
            assert component != null;
            getElement().appendChild(component.getElement());
            getElement().appendChild(getSpacer());
        }
    }

    private Element getSpacer() {
        Element spacer = ElementFactory.createDiv();
        spacer.getStyle().set("marginTop", "10px");
        return spacer;
    }
}
