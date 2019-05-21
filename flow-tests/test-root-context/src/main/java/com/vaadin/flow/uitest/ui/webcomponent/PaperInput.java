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
package com.vaadin.flow.uitest.ui.webcomponent;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.PropertyDescriptor;
import com.vaadin.flow.component.PropertyDescriptors;
import com.vaadin.flow.component.Synchronize;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.dependency.HtmlImport;
import com.vaadin.flow.component.dependency.JsModule;
import com.vaadin.flow.component.dependency.NpmPackage;

@Tag("paper-input")
@HtmlImport("frontend://bower_components/paper-input/paper-input.html")
@NpmPackage(value = "@polymer/paper-input", version = "3.0.2")
@JsModule("@polymer/paper-input/paper-input.js")
public class PaperInput extends Component {
    private static final PropertyDescriptor<String, String> valueDescriptor = PropertyDescriptors
            .propertyWithDefault("value", "");

    public PaperInput() {
        // (this public no-arg constructor is required so that Flow can
        // instantiate beans of this type
        // when they are bound to template elements via the @Id() annotation)
    }

    public PaperInput(String value) {
        setValue(value);
    }

    @Synchronize("value-changed")
    public String getValue() {
        return get(valueDescriptor);
    }

    @Synchronize("invalid-changed")
    public String getInvalid() {
        return getElement().getProperty("invalid");
    }

    public void setValue(String value) {
        set(valueDescriptor, value);
    }
}