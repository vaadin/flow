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

import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.dependency.HtmlImport;
import com.vaadin.flow.component.dependency.JsModule;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.polymertemplate.PolymerTemplate;
import com.vaadin.flow.dom.PropertyChangeEvent;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.uitest.ui.AbstractDivView;

@Route("com.vaadin.flow.uitest.ui.template.PolymerPropertyChangeEventView")
public class PolymerPropertyChangeEventView extends AbstractDivView {

    @Tag("property-change")
    @HtmlImport("frontend://com/vaadin/flow/uitest/ui/template/PolymerPropertyChange.html")
    @JsModule("PolymerPropertyChange.js")
    public static class PolymerPropertyChange extends PolymerTemplate<Message> {

    }

    public PolymerPropertyChangeEventView() {
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
