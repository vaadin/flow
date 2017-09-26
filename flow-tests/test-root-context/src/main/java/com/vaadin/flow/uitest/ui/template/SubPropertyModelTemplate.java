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
package com.vaadin.flow.uitest.ui.template;

import com.vaadin.ui.polymertemplate.EventHandler;
import com.vaadin.ui.common.HtmlImport;
import com.vaadin.ui.polymertemplate.ModelItem;
import com.vaadin.ui.html.Div;
import com.vaadin.ui.polymertemplate.PolymerTemplate;
import com.vaadin.flow.model.TemplateModel;
import com.vaadin.flow.uitest.ui.template.SubPropertyModelTemplate.SubPropertyModel;
import com.vaadin.ui.Tag;
import com.vaadin.ui.common.HasComponents;

@Tag("sub-property-model")
@HtmlImport("/com/vaadin/flow/uitest/ui/template/SubPropertyModel.html")
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
