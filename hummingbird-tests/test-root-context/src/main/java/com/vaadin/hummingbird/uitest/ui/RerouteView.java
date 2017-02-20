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
package com.vaadin.hummingbird.uitest.ui;

import com.vaadin.annotations.Tag;
import com.vaadin.hummingbird.dom.EventRegistrationHandle;
import com.vaadin.hummingbird.event.ComponentEventListener;
import com.vaadin.hummingbird.html.Button;
import com.vaadin.hummingbird.html.HtmlContainer;
import com.vaadin.hummingbird.html.Input;
import com.vaadin.hummingbird.html.Label;
import com.vaadin.hummingbird.html.event.ChangeEvent;
import com.vaadin.hummingbird.html.event.ChangeNotifier;
import com.vaadin.hummingbird.router.LocationChangeEvent;

public class RerouteView extends AbstractDivView {

    boolean reroute = false;

    public RerouteView() {
        Button button = new Button("Navigate to here");
        button.setId("navigate");
        button.addClickListener(e -> {
            button.getUI().ifPresent(ui -> ui.navigateTo(
                    "com.vaadin.hummingbird.uitest.ui.RerouteView"));
        });

        CheckBox checkbox = new CheckBox("RerouteToError");
        checkbox.setId("check");
        checkbox.addChangeListener(event -> {
            reroute = checkbox.isChecked();
        });
        add(button);
        add(checkbox);
    }

    private void setReroute(boolean reroute) {
        this.reroute = reroute;
    }

    @Override
    public void onLocationChange(LocationChangeEvent event) {
        if (reroute) {
            event.rerouteToErrorView();
        }

        super.onLocationChange(event);
    }

    @Tag(Tag.DIV)
    public class CheckBox extends HtmlContainer implements ChangeNotifier {

        Input input;
        Label captionLabel;

        public CheckBox() {
            input = new Input();
            input.getElement().setAttribute("type", "checkbox");
            input.getElement().synchronizeProperty("checked", "false");
            add(input);
        }

        public CheckBox(String caption) {
            this();
            captionLabel = new Label(caption);
            add(captionLabel);
        }

        @Override
        public EventRegistrationHandle addChangeListener(
                ComponentEventListener<ChangeEvent> listener) {
            return input.addChangeListener(listener);
        }

        public boolean isChecked() {
            return Boolean.parseBoolean(input.getElement().getProperty("checked"));
        }
    }

}
