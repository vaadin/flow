/**
 * Copyright (C) 2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See {@literal <https://vaadin.com/commercial-license-and-service-terms>}  for the full
 * license.
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
