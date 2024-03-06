/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.testutil;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.HasValidation;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.NativeButton;

/**
 * Abstract view class for testing validation with components that implement
 * {@link HasValidation}.
 *
 * @since 1.0
 */
public abstract class ValidationTestView extends Div {
    /**
     * Default constructor.
     */
    public ValidationTestView() {
        initView();
    }

    private void initView() {
        HasValidation field = getValidationComponent();
        ((Component) field).setId("field");
        add(((Component) field));

        NativeButton button = new NativeButton("Make the input invalid");
        button.setId("invalidate");
        button.addClickListener(event -> {
            field.setErrorMessage("Invalidated from server");
            field.setInvalid(true);
        });
        add(button);

        button = new NativeButton("Make the input valid");
        button.setId("validate");
        button.addClickListener(event -> {
            field.setErrorMessage(null);
            field.setInvalid(false);
        });
        add(button);
    }

    /**
     * Gets the component to be tested.
     *
     * @return a component that implements {@link HasValidation}
     */
    protected abstract HasValidation getValidationComponent();
}
