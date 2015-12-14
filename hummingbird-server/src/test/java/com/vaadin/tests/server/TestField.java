package com.vaadin.tests.server;

import com.vaadin.annotations.Tag;
import com.vaadin.ui.AbstractField;

@Tag("input")
public class TestField extends AbstractField<String> {

    @Override
    public Class<String> getType() {
        return String.class;
    }

}
