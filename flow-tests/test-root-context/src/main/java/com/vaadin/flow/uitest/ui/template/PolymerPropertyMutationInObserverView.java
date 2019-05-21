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
import com.vaadin.flow.router.Route;
import com.vaadin.flow.uitest.ui.AbstractDivView;

@Route("com.vaadin.flow.uitest.ui.template.PolymerPropertyMutationInObserverView")
public class PolymerPropertyMutationInObserverView extends AbstractDivView {

    @Tag("property-mutation-in-observer")
    @HtmlImport("frontend://com/vaadin/flow/uitest/ui/template/PolymerPropertyMutationInObserver.html")
    @JsModule("PolymerPropertyMutationInObserver.js")
    public static class PolymerPropertyMutationInObserver
            extends PolymerTemplate<Message> {

        public void setText(String text) {
            getModel().setText(text);
        }

        private Div getValueDiv(String eventOldValue, String eventValue) {
            Div div = new Div();
            div.setText(String.format(
                    "Event old value: %s, event value: %s, current model value: %s",
                    eventOldValue, eventValue, getModel().getText()));
            div.addClassName("model-value");
            return div;
        }
    }

    public PolymerPropertyMutationInObserverView() {
        PolymerPropertyMutationInObserver template = new PolymerPropertyMutationInObserver();
        template.setId("template");
        template.getElement().addPropertyChangeListener("text",
                event -> add(template.getValueDiv((String) event.getOldValue(),
                        (String) event.getValue())));
        template.setText("initially set value");
        add(template);
    }

}
