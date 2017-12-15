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
package com.vaadin.flow.uitest.ui;

import java.io.Serializable;

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Input;
import com.vaadin.flow.component.html.NativeButton;
import com.vaadin.flow.nodefeature.ModelMap;
import com.vaadin.flow.nodefeature.TemplateMap;
import com.vaadin.flow.polymertemplate.Id;
import com.vaadin.flow.template.angular.model.TemplateModel;
import com.vaadin.flow.uitest.servlet.ViewTestLayout;
import com.vaadin.flow.router.Route;
import com.vaadin.ui.AngularTemplate;
import com.vaadin.ui.common.ClientDelegate;

@Route(value = "com.vaadin.flow.uitest.ui.BasicTemplateView", layout = ViewTestLayout.class)
public class BasicTemplateView extends AngularTemplate {

    public interface Model extends TemplateModel {
        // Not actually always a string any more, but must define some type for
        // the value to be considered present
        String getModelValue();

        String getFoo();
    }

    @Id("container")
    private Div container;
    @Id("clearModel")
    private NativeButton clearModel;

    @Id("input")
    private Input input;

    public BasicTemplateView() {
        assert container != null;

        setModelValue(null);
        setModelValue("foo", null);

        NativeButton button = new NativeButton(
                "Element added to template (click to remove)");
        button.addClickListener(e -> container.remove(button));
        container.add(button);

        NativeButton childSlotContent = new NativeButton(
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

    @Override
    protected Model getModel() {
        return (Model) super.getModel();
    }

    @ClientDelegate
    private void setModelText() {
        setModelValue("text");
    }

    @ClientDelegate
    private void setModelBoolean() {
        setModelValue(Boolean.FALSE);
    }

    @ClientDelegate
    private void setAttributes() {
        input.setValue("updated");
        input.setPlaceholder("placeholder");
    }

    @ClientDelegate
    private void updateAttributeBinding() {
        getElement().getNode().getFeature(ModelMap.class).setValue("foo",
                "bar");
    }

    private void setModelValue(Serializable value) {
        // Directly manipulating the node feature to enable testing the same
        // bindings with different types
        setModelValue("modelValue", value);
    }

    private void setModelValue(String property, Serializable value) {
        getElement().getNode().getFeature(ModelMap.class).setValue(property,
                value);
    }
}
