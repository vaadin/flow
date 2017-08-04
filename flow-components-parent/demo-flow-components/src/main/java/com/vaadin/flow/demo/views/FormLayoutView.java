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
import com.vaadin.flow.html.Div;
import com.vaadin.flow.html.H3;
import com.vaadin.ui.FormLayout;
import com.vaadin.ui.FormLayout.ResponsiveStep;
import com.vaadin.ui.FormLayout.VaadinFormItem;
import com.vaadin.ui.TextField;

/**
 * Demo view for {@link FormLayout}.
 * 
 * @author Vaadin Ltd
 */
@ComponentDemo(name = "Vaadin Form Layout", href = "vaadin-form-layout")
public class FormLayoutView extends DemoView {

    @Override
    void initView() {
        // @formatter:off
        // begin-source-example
        // source-example-heading: A form layout with custom responsive layouting
        FormLayout nameLayout = new FormLayout();

        TextField titleField = new TextField()
                .setLabel("Title")
                .setPlaceholder("Sir");
        TextField firstNameField = new TextField()
                .setLabel("First name")
                .setPlaceholder("John");
        TextField lastNameField = new TextField()
                .setLabel("Last name")
                .setPlaceholder("Doe");

        nameLayout.add(titleField, firstNameField, lastNameField);

        nameLayout.setResponsiveSteps(
                new ResponsiveStep("0", 1),
                new ResponsiveStep("20em", 2),
                new ResponsiveStep("22em", 3));
        // end-source-example
        // @formatter:on

        // begin-source-example
        // source-example-heading: A form layout with fields wrapped in form items
        FormLayout layoutWithFormItems = new FormLayout();

        VaadinFormItem firstItem = new VaadinFormItem(
                new TextField().setPlaceholder("John"));
        VaadinFormItem secondItem = new VaadinFormItem(
                new TextField().setPlaceholder("Doe"));

        Div firstItemLabelComponent = new Div();
        firstItemLabelComponent.setText("First name");

        Div secondItemLabelComponent = new Div();
        secondItemLabelComponent.setText("Last name");

        firstItem.addToLabel(firstItemLabelComponent);
        secondItem.addToLabel(secondItemLabelComponent);

        layoutWithFormItems.add(firstItem, secondItem);
        // end-source-example

        add(new H3("A form layout with custom responsive layouting"),
                nameLayout);
        add(new H3("A form layout with fields wrapped in form items"),
                layoutWithFormItems);
    }
}
