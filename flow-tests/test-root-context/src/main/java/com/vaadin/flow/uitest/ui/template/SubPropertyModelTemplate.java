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

import com.vaadin.flow.component.HasComponents;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.dependency.HtmlImport;
import com.vaadin.flow.component.dependency.JsModule;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.polymertemplate.EventHandler;
import com.vaadin.flow.component.polymertemplate.ModelItem;
import com.vaadin.flow.component.polymertemplate.PolymerTemplate;
import com.vaadin.flow.templatemodel.TemplateModel;
import com.vaadin.flow.uitest.ui.template.SubPropertyModelTemplate.SubPropertyModel;

@Tag("sub-property-model")
@HtmlImport("frontend://com/vaadin/flow/uitest/ui/template/SubPropertyModel.html")
@JsModule("SubPropertyModel.js")
public class SubPropertyModelTemplate
        extends PolymerTemplate<SubPropertyModel> {

    public interface Status {
        void setMessage(String message);

        String getMessage();
    }

    public interface SubPropertyModel extends TemplateModel {
        void setStatus(Status status);
    }

    public SubPropertyModelTemplate() {
        setMessage("message");
    }

    @EventHandler
    private void update() {
        setMessage("Updated");
    }

    @EventHandler
    private void sync() {
        Div div = new Div();
        div.setId("synced-msg");
        div.setText(getStatus().getMessage());
        ((HasComponents) getParent().get()).add(div);
    }

    @EventHandler
    private void valueUpdated() {
        Div div = new Div();
        div.setId("value-update");
        div.setText(getStatus().getMessage());
        ((HasComponents) getParent().get()).add(div);
    }

    @EventHandler
    private void click(@ModelItem("status") Status statusItem) {
        Div div = new Div();
        div.setId("statusClick");
        div.setText(statusItem.getMessage());
        ((HasComponents) getParent().get()).add(div);
    }

    private void setMessage(String message) {
        getStatus().setMessage(message);
    }

    private Status getStatus() {
        return getModel().getProxy("status", Status.class);
    }
}
