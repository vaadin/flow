package com.vaadin.tests.components.caption;

import com.vaadin.server.UserError;
import com.vaadin.tests.components.TestBase;
import com.vaadin.ui.TextField;

public class EmptyCaptions extends TestBase {

    @Override
    protected void setup() {
        TextField tf;

        tf = new TextField(null, "Null caption");
        add(tf);

        tf = new TextField("", "Empty caption");
        add(tf);

        tf = new TextField(" ", "Space as caption");
        add(tf);

        tf = new TextField(null, "Null caption, required");
        tf.setRequired(true);
        add(tf);
        tf = new TextField("", "Empty caption, required");
        tf.setRequired(true);
        add(tf);
        tf = new TextField(" ", "Space as caption, required");
        tf.setRequired(true);
        add(tf);

        tf = new TextField(null, "Null caption, error");
        tf.setComponentError(new UserError("error"));
        add(tf);

        tf = new TextField("", "Empty caption, error");
        tf.setComponentError(new UserError("error"));
        add(tf);

        tf = new TextField(" ", "Space as caption, error");
        tf.setComponentError(new UserError("error"));
        add(tf);

    }

    @Override
    protected String getTestDescription() {
        return "Null caption should never use space while a non-null caption always should use space.";
    }

    @Override
    protected Integer getTicketNumber() {
        return 3846;
    }

}
