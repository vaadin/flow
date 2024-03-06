/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.webcomponent;

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Paragraph;

public class DefaultValueInitializationComponent extends Div {
    private int value = 0;
    private int updateCounter = 0;
    private Paragraph valueParagraph;
    private Paragraph updateParagraph;

    public DefaultValueInitializationComponent() {
        valueParagraph = new Paragraph();
        valueParagraph.setId("value");
        updateParagraph = new Paragraph();
        updateParagraph.setId("counter");
        updateParagraphs();
        add(valueParagraph, updateParagraph);
    }

    public void updateValue(int newValue) {
        value = newValue;
        updateCounter++;
        updateParagraphs();
    }

    private void updateParagraphs() {
        valueParagraph.setText(Integer.toString(value));
        updateParagraph.setText(Integer.toString(updateCounter));
    }
}
