package com.vaadin.flow.components.it;

import com.vaadin.ui.Component;
import com.vaadin.ui.button.Button;
import com.vaadin.ui.common.HasValidation;

public abstract class ValidationTestView extends TestView {

    protected abstract HasValidation getValidationComponent();

    public ValidationTestView() {
        initView();
    }

    private void initView() {
        HasValidation field = getValidationComponent();
        ((Component) field).setId("field");
        add(((Component) field));

        Button button = new Button("Make the input invalid");
        button.setId("invalidate");
        button.addClickListener(event -> {
            field.setErrorMessage("Invalidated from server");
            field.setInvalid(true);
        });
        add(button);

        button = new Button("Make the input valid");
        button.setId("validate");
        button.addClickListener(event -> {
            field.setErrorMessage(null);
            field.setInvalid(false);
        });
        add(button);
    }

}
