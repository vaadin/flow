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
import com.vaadin.hummingbird.dom.Element;
import com.vaadin.hummingbird.dom.ElementFactory;
import com.vaadin.hummingbird.html.Button;
import com.vaadin.hummingbird.nodefeature.ModelMap;
import com.vaadin.hummingbird.nodefeature.TemplateMap;
import com.vaadin.hummingbird.router.View;
import com.vaadin.ui.Template;

public class BasicTemplateView extends Template implements View {
    public BasicTemplateView() {
        // Child 0 is whitespace, child 1 is bar, child 2 is more whitespace
        Element container = getElement().getChild(3);
        assert "container".equals(container.getAttribute("id"));

        Element button = ElementFactory
                .createButton("Element added to template (click to remove)");
        button.addEventListener("click", e -> button.removeFromParent());

        container.appendChild(button);

        Button childSlotContent = new Button(
                "Child slot content (click to remove)");
        childSlotContent.addClassName("childSlotContent");

        // Will introduce a nicer API in a separate patch
        childSlotContent.addClickListener(e -> {
            getElement().getNode().getFeature(TemplateMap.class).setChild(null);
        });
        getElement().getNode().getFeature(TemplateMap.class)
                .setChild(childSlotContent.getElement().getNode());
    }

    @EventHandler
    private void clearModel() {
        setModelValue(null);
    }

    @EventHandler
    private void setModelText() {
        setModelValue("text");
    }

    @EventHandler
    private void setModelBoolean() {
        setModelValue(Boolean.FALSE);
    }

    private void setModelValue(Serializable value) {
        // Directly manipulating the node feature to enable testing the same
        // bindings with different types
        getElement().getNode().getFeature(ModelMap.class).setValue("modelValue",
                value);
    }
}
