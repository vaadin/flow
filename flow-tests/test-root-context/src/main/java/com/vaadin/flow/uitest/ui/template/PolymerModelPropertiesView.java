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

import com.vaadin.annotations.DomEvent;
import com.vaadin.annotations.EventHandler;
import com.vaadin.annotations.HtmlImport;
import com.vaadin.annotations.Tag;
import com.vaadin.ui.html.Div;
import com.vaadin.flow.router.View;
import com.vaadin.flow.template.PolymerTemplate;
import com.vaadin.ui.AttachEvent;
import com.vaadin.ui.ComponentEvent;

@Tag("model-properties")
@HtmlImport("/com/vaadin/flow/uitest/ui/template/PolymerModelProperties.html")
public class PolymerModelPropertiesView extends PolymerTemplate<Message> implements View {

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
