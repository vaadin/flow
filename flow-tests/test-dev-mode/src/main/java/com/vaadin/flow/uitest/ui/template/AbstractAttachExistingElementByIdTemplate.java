/*
 * Copyright 2000-2019 Vaadin Ltd.
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

import com.vaadin.flow.component.html.Input;
import com.vaadin.flow.component.html.Label;
import com.vaadin.flow.component.polymertemplate.EventHandler;
import com.vaadin.flow.component.polymertemplate.Id;
import com.vaadin.flow.component.polymertemplate.PolymerTemplate;
import com.vaadin.flow.templatemodel.TemplateModel;

public abstract class AbstractAttachExistingElementByIdTemplate
        extends PolymerTemplate<TemplateModel> {

    @Id("input")
    private Input input;

    @Id("label")
    private Label label;

    protected AbstractAttachExistingElementByIdTemplate(String id) {
        setId(id);
        input.setPlaceholder("Type here to update label");
        label.setText("default");
    }

    @EventHandler
    private void clear() {
        label.setText("default");
        input.setValue("");
    }

    @EventHandler
    private void valueChange() {
        label.setText("Text from input " + input.getValue());
    }

}
