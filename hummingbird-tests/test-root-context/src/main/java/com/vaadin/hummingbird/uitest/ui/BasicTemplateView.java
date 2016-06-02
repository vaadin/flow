/*
 * Copyright 2000-2016 Vaadin Ltd.
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
package com.vaadin.hummingbird.uitest.ui;

import java.io.Serializable;

import com.vaadin.annotations.EventHandler;
import com.vaadin.annotations.Id;
import com.vaadin.hummingbird.html.Button;
import com.vaadin.hummingbird.html.Div;
import com.vaadin.hummingbird.html.Input;
import com.vaadin.hummingbird.nodefeature.ModelMap;
import com.vaadin.hummingbird.nodefeature.TemplateMap;
import com.vaadin.hummingbird.router.View;
import com.vaadin.ui.Template;

public class BasicTemplateView extends Template implements View {

    @Id("container")
    private Div container;
    @Id("clearModel")
    private Button clearModel;

    @Id("input")
    private Input input;

    public BasicTemplateView() {
        assert container != null;

        Button button = new Button(
                "Element added to template (click to remove)");
        button.addClickListener(e -> container.remove(button));
        container.add(button);

        Button childSlotContent = new Button(
                "Child slot content (click to remove)");
        childSlotContent.addClassName("childSlotContent");

        // Will introduce a nicer API in a separate patch
        childSlotContent.addClickListener(e -> {
            getElement().getNode().getFeature(TemplateMap.class).setChild(null);
        });
        getElement().getNode().getFeature(TemplateMap.class)
                .setChild(childSlotContent.getElement().getNode());

        clearModel.addClickListener(e -> setModelValue(null));
    }

    @EventHandler
    private void setModelText() {
        setModelValue("text");
    }

    @EventHandler
    private void setModelBoolean() {
        setModelValue(Boolean.FALSE);
    }

    @EventHandler
    private void setAttributes() {
        input.setValue("updated");
        input.setPlaceholder("placeholder");
    }

    @EventHandler
    private void updateAttributeBinding() {
        getElement().getNode().getFeature(ModelMap.class).setValue("foo",
                "bar");
    }

    private void setModelValue(Serializable value) {
        // Directly manipulating the node feature to enable testing the same
        // bindings with different types
        getElement().getNode().getFeature(ModelMap.class).setValue("modelValue",
                value);
    }
}
