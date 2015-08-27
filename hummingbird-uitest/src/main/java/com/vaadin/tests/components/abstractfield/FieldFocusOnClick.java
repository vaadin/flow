package com.vaadin.tests.components.abstractfield;

import java.util.Arrays;

import com.vaadin.server.VaadinRequest;
import com.vaadin.tests.components.AbstractTestUI;
import com.vaadin.ui.CheckBox;
import com.vaadin.ui.NativeButton;
import com.vaadin.ui.OptionGroup;
import com.vaadin.ui.TextField;

public class FieldFocusOnClick extends AbstractTestUI {

    @Override
    protected void setup(VaadinRequest request) {
        add(new TextField(null, "TextField"));
        add(new CheckBox("CheckBox"));
        add(
                new OptionGroup(null, Arrays.asList("Option 1", "Option 2")));
        add(new NativeButton("NativeButton"));
    }

    @Override
    protected String getTestDescription() {
        return "Webkit doesn't focus non-text input elements when clicked";
    }

    @Override
    protected Integer getTicketNumber() {
        return 11854;
    }
}
