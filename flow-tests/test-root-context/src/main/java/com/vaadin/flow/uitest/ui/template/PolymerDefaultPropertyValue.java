/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.uitest.ui.template;

import com.vaadin.flow.component.PropertyDescriptor;
import com.vaadin.flow.component.PropertyDescriptors;
import com.vaadin.flow.component.Synchronize;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.dependency.JsModule;
import com.vaadin.flow.component.polymertemplate.PolymerTemplate;
import com.vaadin.flow.templatemodel.TemplateModel;
import com.vaadin.flow.uitest.ui.template.PolymerDefaultPropertyValue.MyModel;

@Tag("default-property")
@JsModule("./PolymerDefaultPropertyValue.js")
public class PolymerDefaultPropertyValue extends PolymerTemplate<MyModel> {

    public interface MyModel extends TemplateModel {
        void setText(String text);

        void setName(String name);
    }

    private static final PropertyDescriptor<String, String> msgDescriptor = PropertyDescriptors
            .propertyWithDefault("message", "");

    public PolymerDefaultPropertyValue() {
        getModel().setText("foo");
        setMessage("updated-message");
    }

    @Synchronize("email-changed")
    public String getEmail() {
        return getElement().getProperty("email");
    }

    @Synchronize("message-changed")
    public String getMessage() {
        return get(msgDescriptor);
    }

    public void setMessage(String value) {
        set(msgDescriptor, value);
    }

}
