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

import com.vaadin.flow.demo.ComponentDemo;
import com.vaadin.generated.vaadin.text.field.GeneratedVaadinTextField;
import com.vaadin.ui.VaadinButton;
import com.vaadin.ui.VaadinFormLayout;
import com.vaadin.ui.VaadinFormLayout.ResponsiveStep;
import com.vaadin.ui.VaadinFormLayout.ResponsiveStep.LabelsPosition;
import com.vaadin.ui.VaadinFormLayout.VaadinFormItem;

/**
 * Demo view for {@link VaadinFormLayout}.
 * 
 * @author Vaadin Ltd
 */
@ComponentDemo(name = "Vaadin Form Layout", href = "vaadin-form-layout")
public class VaadinFormLayoutView extends DemoView {

    @Override
    void initView() {
        // begin-source-example
        VaadinFormLayout layout = new VaadinFormLayout();
        GeneratedVaadinTextField tf = new GeneratedVaadinTextField()
                .setLabel("tf1").setPlaceholder("tf1");
        GeneratedVaadinTextField tf2 = new GeneratedVaadinTextField()
                .setLabel("tf2").setPlaceholder("tf2");
        layout.add(tf, tf2);
        layout.setResponsiveSteps(
                new ResponsiveStep("0px", 2, LabelsPosition.ASIDE));
        // end-source-example

        // begin-source-example
        VaadinFormLayout layout2 = new VaadinFormLayout();
        VaadinFormItem item1 = new VaadinFormItem(new GeneratedVaadinTextField());
        VaadinFormItem item2 = new VaadinFormItem(new GeneratedVaadinTextField());
        item1.addToLabel(new VaadinButton("Button in label :o WOAH"));
        item2.addToLabel(new VaadinButton("And another one!"), new VaadinButton("And another one!"));
        layout2.add(item1, item2);
        // end-source-example

        add(layout);
        add(layout2);
    }
}
