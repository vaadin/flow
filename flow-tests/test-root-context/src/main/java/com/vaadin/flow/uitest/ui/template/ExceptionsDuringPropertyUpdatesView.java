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

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.HasComponents;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.Text;
import com.vaadin.flow.component.dependency.HtmlImport;
import com.vaadin.flow.component.dependency.JsModule;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.polymertemplate.PolymerTemplate;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.templatemodel.AllowClientUpdates;
import com.vaadin.flow.templatemodel.TemplateModel;
import com.vaadin.flow.uitest.servlet.ViewTestLayout;
import com.vaadin.flow.uitest.ui.template.ExceptionsDuringPropertyUpdatesView.ExceptionsDuringPropertyUpdatesModel;

@Tag("exceptions-property-update")
@HtmlImport("frontend://com/vaadin/flow/uitest/ui/template/ExceptionsDuringPropertyUpdates.html")
@Route(value = "com.vaadin.flow.uitest.ui.template.ExceptionsDuringPropertyUpdatesView", layout = ViewTestLayout.class)
@JsModule("ExceptionsDuringPropertyUpdates.js")
public class ExceptionsDuringPropertyUpdatesView
        extends PolymerTemplate<ExceptionsDuringPropertyUpdatesModel>
        implements HasComponents {

    public interface ExceptionsDuringPropertyUpdatesModel
            extends TemplateModel {
        void setText(String text);

        @AllowClientUpdates
        String getText();

        void setName(String name);

        @AllowClientUpdates
        String getName();

        void setTitle(String title);

        @AllowClientUpdates
        String getTitle();
    }

    public ExceptionsDuringPropertyUpdatesView() {
        Div msg = new Div();
        msg.setId("message");

        add(msg);

        getModel().setText("a");

        getElement().addPropertyChangeListener("text", event -> {
            throw new RuntimeException(
                    "Intentional exception in property sync handler for 'text'");
        });
        getElement().addPropertyChangeListener("title", event -> {
            throw new IllegalStateException(
                    "Intentional exception in property sync handler for 'title'");
        });
        getElement().addPropertyChangeListener("name", event -> msg
                .setText("Name is updated to " + getModel().getName()));
    }

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        if (attachEvent.isInitialAttach()) {
            attachEvent.getSession().setErrorHandler(e -> {
                Div div = new Div(
                        new Text("An error occurred: " + e.getThrowable()));
                div.addClassName("error");
                add(div);
            });
        }
    }
}
