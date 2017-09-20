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

import com.vaadin.annotations.HtmlImport;
import com.vaadin.annotations.Tag;
import com.vaadin.ui.html.Div;
import com.vaadin.ui.polymertemplate.PolymerTemplate;
import com.vaadin.server.VaadinRequest;
import com.vaadin.ui.UI;

public class PolymerPropertyMutationInObserverUI extends UI {

    @Tag("property-mutation-in-observer")
    @HtmlImport("/com/vaadin/flow/uitest/ui/template/PolymerPropertyMutationInObserver.html")
    public static class PolymerPropertyMutationInObserver
            extends PolymerTemplate<Message> {

        public void setText(String text) {
            getModel().setText(text);
        }

        private Div getValueDiv() {
            Div div = new Div();
            div.setText(getModel().getText());
            div.addClassName("model-value");
            return div;
        }
    }

    @Override
    protected void init(VaadinRequest request) {
        PolymerPropertyMutationInObserver template = new PolymerPropertyMutationInObserver();
        template.setId("template");
        template.getElement().addPropertyChangeListener("text",
                event -> add(template.getValueDiv()));
        template.setText("initially set value");
        add(template);
    }
}
