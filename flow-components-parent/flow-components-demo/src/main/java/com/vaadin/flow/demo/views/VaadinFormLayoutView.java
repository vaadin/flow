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
import com.vaadin.flow.html.H3;
import com.vaadin.ui.VaadinButton;
import com.vaadin.ui.VaadinFormLayout;
import com.vaadin.ui.VaadinFormLayout.ResponsiveStep;
import com.vaadin.ui.VaadinFormLayout.VaadinFormItem;
import com.vaadin.ui.VaadinTextField;

/**
 * Demo view for {@link VaadinFormLayout}.
 * 
 * @author Vaadin Ltd
 */
@ComponentDemo(name = "Vaadin Form Layout", href = "vaadin-form-layout")
public class VaadinFormLayoutView extends DemoView {

    @Override
    void initView() {
        // @formatter:off
        // begin-source-example
        // source-example-heading: A form layout with custom responsive layouting
        VaadinFormLayout nameLayout = new VaadinFormLayout();

        VaadinTextField titleField = new VaadinTextField()
                .setLabel("Title")
                .setPlaceholder("Sir");
        VaadinTextField firstNameField = new VaadinTextField()
                .setLabel("First name")
                .setPlaceholder("John");
        VaadinTextField lastNameField = new VaadinTextField()
                .setLabel("Last name")
                .setPlaceholder("Doe");

        nameLayout.add(titleField, firstNameField, lastNameField);

        nameLayout.setResponsiveSteps(
                new ResponsiveStep("0", 1),
                new ResponsiveStep("18em", 2),
                new ResponsiveStep("20em", 3));
        // end-source-example
        // @formatter:on

        // begin-source-example
        // source-example-heading: A form layout with fields wrapped in form items
        VaadinFormLayout layoutWithFormItems = new VaadinFormLayout();

        VaadinFormItem firstItem = new VaadinFormItem(
                new VaadinTextField("First name", "John"));
        VaadinFormItem secondItem = new VaadinFormItem(
                new VaadinTextField("Last name", "Doe"));

        firstItem.addToLabel(
                new VaadinButton("Button inside the label"));
        secondItem.addToLabel(
                new VaadinButton("And another one!"),
                new VaadinButton("And another one!"));

        layoutWithFormItems.add(firstItem, secondItem);
        // end-source-example

        add(new H3("A form layout with custom responsive layouting"),
                nameLayout);
        add(new H3("A form layout with fields wrapped in form items"),
                layoutWithFormItems);
    }
}
