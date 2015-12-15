package com.vaadin.tests.server;

import java.util.Date;

import com.vaadin.ui.AbstractField;

public class TestDateField extends AbstractField<Date> {

    @Override
    public Class getType() {
        return Date.class;
    }

}
