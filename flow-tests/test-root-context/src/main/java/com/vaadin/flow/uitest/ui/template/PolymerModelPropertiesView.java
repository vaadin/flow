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
import com.vaadin.flow.component.ComponentEvent;
import com.vaadin.flow.component.DomEvent;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.dependency.HtmlImport;
import com.vaadin.flow.component.dependency.JsModule;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.polymertemplate.EventHandler;
import com.vaadin.flow.component.polymertemplate.PolymerTemplate;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.uitest.servlet.ViewTestLayout;

@Route(value = "com.vaadin.flow.uitest.ui.template.PolymerModelPropertiesView", layout = ViewTestLayout.class)
@Tag("model-properties")
@HtmlImport("frontend://com/vaadin/flow/uitest/ui/template/PolymerModelProperties.html")
@JsModule("PolymerModelProperties.js")
public class PolymerModelPropertiesView extends PolymerTemplate<Message> {

    @DomEvent("text-changed")
    public static class ValueChangeEvent
            extends ComponentEvent<PolymerModelPropertiesView> {
        public ValueChangeEvent(PolymerModelPropertiesView source,
                boolean fromClient) {
            super(source, fromClient);
        }
    }

    public PolymerModelPropertiesView() {
        setId("template");
        getModel().setText("foo");

        getElement().synchronizeProperty("text", "text-changed");

        addListener(ValueChangeEvent.class, event -> {
            getUI().get().add(addUpdateElement("property-update-event"));
        });
    }

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        getUI().get().add(addUpdateElement("property-value"));
    }

    @EventHandler
    private void valueUpdated() {
        getUI().get().add(addUpdateElement("value-update"));
    }

    private Div addUpdateElement(String id) {
        Div div = new Div();
        div.setText("Property value:" + getElement().getProperty("text")
                + ", model value: " + getModel().getText());
        div.setId(id);
        return div;
    }
}
