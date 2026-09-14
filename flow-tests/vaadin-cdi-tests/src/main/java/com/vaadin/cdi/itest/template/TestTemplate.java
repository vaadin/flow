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
package com.vaadin.cdi.itest.template;

import jakarta.enterprise.event.Event;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;

import com.vaadin.cdi.annotation.CdiComponent;
import com.vaadin.cdi.annotation.UIScoped;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.dependency.JsModule;
import com.vaadin.flow.component.html.Input;
import com.vaadin.flow.component.html.NativeLabel;
import com.vaadin.flow.component.polymertemplate.Id;
import com.vaadin.flow.component.polymertemplate.PolymerTemplate;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.templatemodel.TemplateModel;

@Tag("test-template")
@JsModule("./test-template.js")
@UIScoped
@Route("")
@CdiComponent
public class TestTemplate extends PolymerTemplate<TemplateModel> {
    private @Id("input") Input input;

    private @Id("label") NativeLabel label;

    private @Inject Event<InputChangeEvent> setTextEventTrigger;

    public TestTemplate() {
        input.addValueChangeListener(event -> {
            setTextEventTrigger.fire(new InputChangeEvent(input.getValue()));
        });
    }

    private void onSetText(@Observes InputChangeEvent event) {
        label.setText(event.getText());
    }

    public static class InputChangeEvent {
        private final String text;

        public InputChangeEvent(String text) {
            this.text = text;
        }

        public String getText() {
            return text;
        }
    }
}
