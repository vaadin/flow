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

import com.vaadin.ui.common.HtmlImport;
import com.vaadin.ui.event.PropertyChangeEvent;
import com.vaadin.ui.html.Div;
import com.vaadin.ui.polymertemplate.PolymerTemplate;
import com.vaadin.server.VaadinRequest;
import com.vaadin.ui.Tag;
import com.vaadin.ui.UI;

public class PolymerPropertyChangeEventUI extends UI {

    @Tag("property-change")
    @HtmlImport("/com/vaadin/flow/uitest/ui/template/PolymerPropertyChange.html")
    public static class PolymerPropertyChange extends PolymerTemplate<Message> {

    }

    @Override
    protected void init(VaadinRequest request) {
        PolymerPropertyChange template = new PolymerPropertyChange();
        template.setId("template");
        template.getElement().addPropertyChangeListener("text",
                this::propertyChanged);
        add(template);
    }

    private void propertyChanged(PropertyChangeEvent event) {
        Div div = new Div();
        div.setText("New property value: '" + event.getValue()
                + "', old property value: '" + event.getOldValue() + "'");
        div.addClassName("change-event");
        add(div);
    }
}
