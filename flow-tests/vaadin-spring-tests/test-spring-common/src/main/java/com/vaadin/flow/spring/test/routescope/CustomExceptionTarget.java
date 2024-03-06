/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.spring.test.routescope;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.NativeButton;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.ErrorParameter;
import com.vaadin.flow.router.HasErrorParameter;

public class CustomExceptionTarget extends Div
        implements HasErrorParameter<CustomException> {

    @Autowired
    private ApplicationContext context;

    private boolean isSubDiv;

    private com.vaadin.flow.component.Component current;

    @Override
    public int setErrorParameter(BeforeEnterEvent event,
            ErrorParameter<CustomException> parameter) {
        NativeButton button = new NativeButton("switch content", ev -> {
            remove(current);
            if (isSubDiv) {
                current = context.getBean(CustomExceptionSubButton.class);
            } else {
                current = context.getBean(CustomExceptionSubDiv.class);
            }
            add(current);
            isSubDiv = !isSubDiv;
        });
        button.setId("switch-content");
        add(button);
        current = context.getBean(CustomExceptionSubButton.class);
        add(current);
        return 503;
    }

}
