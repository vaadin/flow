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
package com.vaadin.hummingbird.uitest.ui.template;

import com.vaadin.annotations.DomEvent;
import com.vaadin.annotations.EventHandler;
import com.vaadin.annotations.HtmlImport;
import com.vaadin.annotations.Tag;
import com.vaadin.annotations.WebComponents;
import com.vaadin.hummingbird.html.Div;
import com.vaadin.hummingbird.template.PolymerTemplate;
import com.vaadin.hummingbird.template.model.TemplateModel;
import com.vaadin.server.VaadinRequest;
import com.vaadin.ui.AttachEvent;
import com.vaadin.ui.ComponentEvent;
import com.vaadin.ui.UI;

@WebComponents(1)
public class PolymerModelPropertiesUI extends UI {

    @DomEvent("text-changed")
    public static class ValueChangeEvent
            extends ComponentEvent<PolymerModelProperties> {
        public ValueChangeEvent(PolymerModelProperties source,
                boolean fromClient) {
            super(source, fromClient);
        }
    }

    public static interface Message extends TemplateModel {
        void setText(String text);

        String getText();
    }

    @Tag("model-properties")
    @HtmlImport("/com/vaadin/hummingbird/uitest/ui/template/PolymerModelProperties.html")
    public static class PolymerModelProperties
            extends PolymerTemplate<Message> {

        public PolymerModelProperties() {
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

    @Override
    protected void init(VaadinRequest request) {
        PolymerModelProperties template = new PolymerModelProperties();
        template.setId("template");
        add(template);
    }
}
