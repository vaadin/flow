package com.vaadin.flow.uitest.ui;

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Paragraph;

import java.io.Serializable;

public abstract class AbstractDebounceSynchronizeView extends AbstractDivView {

    protected final static int CHANGE_TIMEOUT = 1000;

    private final Div messages = new Div();

    protected void addChangeMessagesDiv() {
        messages.getElement().setAttribute("id", "messages");
        add(messages);
    }

    protected void addChangeMessage(Serializable value) {
        messages.add(new Paragraph("Value: " + value));
    }
}
