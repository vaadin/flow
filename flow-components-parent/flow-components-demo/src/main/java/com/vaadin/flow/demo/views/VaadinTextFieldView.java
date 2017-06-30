/*
 * Copyright 2000-2017 Vaadin Ltd.
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
package com.vaadin.flow.demo.views;

import com.vaadin.components.vaadin.text.field.VaadinTextField;
import com.vaadin.flow.demo.ComponentDemo;
import com.vaadin.flow.demo.SourceContent;

/**
 * View for {@link VaadinTextField} demo.
 */
@ComponentDemo(name = "Vaadin Text Field", href = "vaadin-text-field")
public class VaadinTextFieldView extends DemoView {
    @Override
    void initView() {
        VaadinTextField textField = new VaadinTextField();
        textField.setLabel("Text field label");
        add(textField);
    }

    @Override
    public void populateSources(SourceContent container) {
        container.addCode("VaadinTextField textField = new VaadinTextField();\n"
                + "textField.setLabel(\"Text field label\");\n"
                + "layoutContainer.add(textField);");
    }
}
