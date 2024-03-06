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
import org.springframework.stereotype.Component;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.dependency.JsModule;
import com.vaadin.flow.component.polymertemplate.PolymerTemplate;
import com.vaadin.flow.spring.annotation.VaadinSessionScope;
import com.vaadin.flow.templatemodel.TemplateModel;

@Tag("child-template")
@JsModule("./ChildTemplate.js")
public class ChildTemplate extends PolymerTemplate<TemplateModel> {

    @Component
    @VaadinSessionScope
    public static class BackendImpl implements Backend {

        @Override
        public String getMessage() {
            return "foo";
        }
    }

    public interface Backend {
        String getMessage();
    }

    @Autowired
    private Backend backend;

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        getElement().setProperty("message", backend.getMessage());
    }
}
