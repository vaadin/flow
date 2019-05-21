/*
 * Copyright 2000-2018 Vaadin Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.vaadin.flow.uitest.ui.template;

import com.vaadin.flow.component.PropertyDescriptor;
import com.vaadin.flow.component.PropertyDescriptors;
import com.vaadin.flow.component.Synchronize;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.dependency.HtmlImport;
import com.vaadin.flow.component.dependency.JsModule;
import com.vaadin.flow.component.polymertemplate.PolymerTemplate;
import com.vaadin.flow.templatemodel.TemplateModel;
import com.vaadin.flow.uitest.ui.template.PolymerDefaultPropertyValue.MyModel;

@Tag("default-property")
@HtmlImport("frontend://com/vaadin/flow/uitest/ui/template/PolymerDefaultPropertyValue.html")
@JsModule("PolymerDefaultPropertyValue.js")
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
