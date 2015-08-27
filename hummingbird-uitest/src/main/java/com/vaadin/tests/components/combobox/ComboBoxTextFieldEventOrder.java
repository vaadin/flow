package com.vaadin.tests.components.combobox;

import java.util.Arrays;

import com.vaadin.data.Property.ValueChangeEvent;
import com.vaadin.data.Property.ValueChangeListener;
import com.vaadin.tests.components.TestBase;
import com.vaadin.ui.ComboBox;
import com.vaadin.ui.TextField;

public class ComboBoxTextFieldEventOrder extends TestBase {

    @Override
    protected void setup() {
        TextField textField = new TextField("text field");
        final ComboBox select = new ComboBox("select",
                Arrays.asList("1", "2", "3", "4"));
        textField.addValueChangeListener(new ValueChangeListener() {
            @Override
            public void valueChange(ValueChangeEvent event) {
                select.addItem(Long.valueOf(select.size() + 1).toString()); // or
                                                                            // just
                                                                            // select.requestRepaint();
            }
        });
        add(textField);
        add(select);
    }

    @Override
    protected String getTestDescription() {
        return "Entering a text in a TextField and then clicking on the button in a ComboBox should cause the TextField value change to be sent first and the ComboBox filtering afterwards. Failure to do so will cause errors if the value change listener modifies the ComboBox";
    }

    @Override
    protected Integer getTicketNumber() {
        return 7481;
    }
}
