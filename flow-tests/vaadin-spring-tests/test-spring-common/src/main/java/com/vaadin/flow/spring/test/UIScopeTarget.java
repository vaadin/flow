/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.spring.test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Label;
import com.vaadin.flow.router.Route;

@Route("ui-scope")
public class UIScopeTarget extends Div {

    @Component
    @Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
    public static class InnerComponent extends Div {

        @Autowired
        private UIScopedBean bean;

        @Override
        protected void onAttach(AttachEvent attachEvent) {
            Label label = new Label(String.valueOf(bean.getUid()));
            label.setId("inner");
            add(label);
        }
    }

    public UIScopeTarget(@Autowired UIScopedBean bean,
            @Autowired InnerComponent component) {
        Label label = new Label(String.valueOf(bean.getUid()));
        label.setId("main");
        add(label);

        add(component);
    }

}
