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
package com.vaadin.flow.uitest.ui;

import com.vaadin.flow.component.HasValue.ValueChangeEvent;
import com.vaadin.flow.component.HasValue.ValueChangeListener;
import com.vaadin.flow.component.HtmlContainer;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.html.Input;
import com.vaadin.flow.component.html.Label;
import com.vaadin.flow.component.html.NativeButton;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.NotFoundException;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.shared.Registration;
import com.vaadin.flow.uitest.servlet.ViewTestLayout;

@Route(value = "com.vaadin.flow.uitest.ui.RerouteView", layout = ViewTestLayout.class)
public class RerouteView extends AbstractDivView {

    boolean reroute = false;

    public RerouteView() {
        NativeButton button = new NativeButton("Navigate to here");
        button.setId("navigate");
        button.addClickListener(e -> {
            button.getUI().ifPresent(
                    ui -> ui.navigate("com.vaadin.flow.uitest.ui.RerouteView"));
        });

        CheckBox checkbox = new CheckBox("RerouteToError");
        checkbox.setId("check");
        checkbox.addValueChangeListener(event -> {
            reroute = checkbox.isChecked();
        });
        add(button);
        add(checkbox);
    }

    private void setReroute(boolean reroute) {
        this.reroute = reroute;
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        if (reroute) {
            event.rerouteToError(NotFoundException.class,
                    "Rerouting to error view");
        }

        super.beforeEnter(event);
    }

    @Tag(Tag.DIV)
    public class CheckBox extends HtmlContainer {

        Input input;
        Label captionLabel;

        public CheckBox() {
            input = new Input();
            input.getElement().setAttribute("type", "checkbox");
            input.getElement().addPropertyChangeListener("checked",
                    "change", event -> {});
            add(input);
        }

        public CheckBox(String caption) {
            this();
            captionLabel = new Label(caption);
            add(captionLabel);
        }

        public Registration addValueChangeListener(
                ValueChangeListener<ValueChangeEvent<String>> listener) {
            return input.addValueChangeListener(listener);
        }

        public boolean isChecked() {
            return Boolean
                    .parseBoolean(input.getElement().getProperty("checked"));
        }
    }

}
