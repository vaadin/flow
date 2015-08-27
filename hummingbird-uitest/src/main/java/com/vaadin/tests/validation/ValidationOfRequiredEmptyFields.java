package com.vaadin.tests.validation;

import com.vaadin.data.Property.ValueChangeEvent;
import com.vaadin.data.Property.ValueChangeListener;
import com.vaadin.data.Validator;
import com.vaadin.data.validator.StringLengthValidator;
import com.vaadin.server.VaadinRequest;
import com.vaadin.tests.components.AbstractTestUI;
import com.vaadin.ui.CheckBox;
import com.vaadin.ui.TextField;

@SuppressWarnings("deprecation")
public class ValidationOfRequiredEmptyFields extends AbstractTestUI {

    private TextField tf;
    private CheckBox requiredInput;
    private TextField requiredErrorInput;

    private Validator stringLengthValidator = new StringLengthValidator(
            "Must be 5-10 chars", 5, 10, false);
    private CheckBox stringLengthValidatorInput;

    @Override
    protected void setup(VaadinRequest request) {
        requiredInput = new CheckBox("Field required");

        requiredInput.addValueChangeListener(new ValueChangeListener() {
            @Override
            public void valueChange(ValueChangeEvent event) {
                tf.setRequired(requiredInput.getValue());
            }
        });

        requiredErrorInput = new TextField("Required error message");

        requiredErrorInput.addValueChangeListener(new ValueChangeListener() {
            @Override
            public void valueChange(ValueChangeEvent event) {
                tf.setRequiredError(requiredErrorInput.getValue());
            }
        });

        stringLengthValidatorInput = new CheckBox("String length validator");

        stringLengthValidatorInput
                .addValueChangeListener(new ValueChangeListener() {

                    @Override
                    public void valueChange(ValueChangeEvent event) {
                        if (stringLengthValidatorInput.getValue()) {
                            tf.addValidator(stringLengthValidator);
                        } else {
                            tf.removeValidator(stringLengthValidator);
                        }
                    }
                });

        tf = new TextField();

        requiredInput.setValue(false);
        requiredErrorInput.setValue("");
        stringLengthValidatorInput.setValue(false);

        add(requiredInput);
        add(requiredErrorInput);
        add(stringLengthValidatorInput);
        add(tf);
    }

    @Override
    protected String getTestDescription() {
        return "Tests that the lower textfield's tooltip displays validation error messages correctly.";
    }

    @Override
    protected Integer getTicketNumber() {
        return 3851;
    }

}
