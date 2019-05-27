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

import java.util.UUID;

import com.vaadin.flow.component.ClientCallable;
import com.vaadin.flow.component.HasComponents;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.dependency.HtmlImport;
import com.vaadin.flow.component.dependency.JsModule;
import com.vaadin.flow.component.html.Label;
import com.vaadin.flow.component.polymertemplate.EventHandler;
import com.vaadin.flow.component.polymertemplate.PolymerTemplate;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.templatemodel.AllowClientUpdates;
import com.vaadin.flow.templatemodel.ClientUpdateMode;
import com.vaadin.flow.templatemodel.TemplateModel;
import com.vaadin.flow.uitest.servlet.ViewTestLayout;

@Tag("updatable-model-properties")
@HtmlImport("frontend://com/vaadin/flow/uitest/ui/template/UpdatableModelProperties.html")
@Route(value = "com.vaadin.flow.uitest.ui.template.UpdatableModelPropertiesView", layout = ViewTestLayout.class)
@JsModule("UpdatableModelProperties.js")
public class UpdatableModelPropertiesView extends
        PolymerTemplate<UpdatableModelPropertiesView.UpdatablePropertiesModel>
        implements HasComponents {

    public interface UpdatablePropertiesModel extends TemplateModel {

        @AllowClientUpdates(ClientUpdateMode.IF_TWO_WAY_BINDING)
        String getName();

        @AllowClientUpdates
        String getEmail();

        @AllowClientUpdates
        void setAge(int age);

        @AllowClientUpdates(ClientUpdateMode.DENY)
        void setText(String text);
    }

    public UpdatableModelPropertiesView() {
        setId("template");

        Label label = new Label();
        label.setId("property-value");
        add(label);

        getElement().addPropertyChangeListener("name",
                event -> label.setText(getModel().getName()));
        getElement().addPropertyChangeListener("email",
                event -> label.setText(getModel().getEmail()));
        getElement().addPropertyChangeListener("age",
                event -> label.setText(getElement().getProperty("age")));
        getElement().addPropertyChangeListener("text",
                event -> label.setText(getElement().getProperty("text")));
    }

    @EventHandler
    private void syncAge() {
        getElement().synchronizeProperty("age", "age-changed");
    }

    @ClientCallable
    private void updateStatus() {
        getElement().setProperty("updateStatus",
                "Update Done " + UUID.randomUUID().toString());
    }
}
