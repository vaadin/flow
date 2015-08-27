package com.vaadin.tests.components.textfield;

import com.vaadin.tests.components.TestBase;
import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.TextField;

public class TextFieldPrimaryStyleName extends TestBase {

    @Override
    protected void setup() {
        final TextField field = new TextField();
        field.setPrimaryStyleName("my-textfield");
        add(field);

        add(new Button("Change primary style name",
                new Button.ClickListener() {
                    @Override
                    public void buttonClick(ClickEvent event) {
                        field.setPrimaryStyleName("my-dynamic-textfield");
                    }
                }));

    }

    @Override
    protected String getTestDescription() {
        return "Textfield should support setting the primary stylename both initially and dynamically";
    }

    @Override
    protected Integer getTicketNumber() {
        return 9896;
    }

}
