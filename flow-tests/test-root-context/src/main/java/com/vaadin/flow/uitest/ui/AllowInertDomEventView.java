/*
 * Copyright 2000-2026 Vaadin Ltd.
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

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.ComponentEvent;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.ComponentUtil;
import com.vaadin.flow.component.DomEvent;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.NativeButton;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.shared.Registration;

@Route(value = "com.vaadin.flow.uitest.ui.AllowInertDomEventView")
public class AllowInertDomEventView extends AbstractDivView {

    public static final String OPEN_MODAL_BUTTON = "open-modal-button";
    public static final String ALLOW_INERT_BUTTON = "allow-inert-button";
    public static final String REGULAR_BUTTON = "regular-button";
    public static final String ALLOW_INERT_EVENT_COUNT = "allow-inert-event-count";
    public static final String REGULAR_EVENT_COUNT = "regular-event-count";

    private int allowInertEventCount = 0;
    private int regularEventCount = 0;
    private Span allowInertCountSpan;
    private Span regularCountSpan;

    @Override
    protected void onShow() {
        // Button with allowInert=true click event
        AllowInertClickButton allowInertButton = new AllowInertClickButton(
                "Allow Inert Click");
        allowInertButton.setId(ALLOW_INERT_BUTTON);
        allowInertButton.addAllowInertClickListener(event -> {
            allowInertEventCount++;
            allowInertCountSpan.setText(String.valueOf(allowInertEventCount));
        });

        // NativeButton with regular click event (allowInert=false by default)
        NativeButton regularButton = new NativeButton("Regular Click",
                event -> {
                    regularEventCount++;
                    regularCountSpan.setText(String.valueOf(regularEventCount));
                });
        regularButton.setId(REGULAR_BUTTON);

        allowInertCountSpan = new Span("0");
        allowInertCountSpan.setId(ALLOW_INERT_EVENT_COUNT);

        regularCountSpan = new Span("0");
        regularCountSpan.setId(REGULAR_EVENT_COUNT);

        NativeButton openModalButton = new NativeButton("Open modal dialog",
                event -> new Dialog().open());
        openModalButton.setId(OPEN_MODAL_BUTTON);

        add(openModalButton);
        add(new Div(allowInertButton, new Span(" Count: "),
                allowInertCountSpan));
        add(new Div(regularButton, new Span(" Count: "), regularCountSpan));
    }

    /**
     * Custom button component with a click event that has allowInert=true.
     */
    @Tag("button")
    public static class AllowInertClickButton extends Component {

        public AllowInertClickButton(String text) {
            getElement().setText(text);
            getElement().getStyle().set("border", "1px solid black");
        }

        public Registration addAllowInertClickListener(
                ComponentEventListener<AllowInertClickEvent> listener) {
            return ComponentUtil.addListener(this, AllowInertClickEvent.class,
                    listener);
        }

        @DomEvent(value = "click", allowInert = true)
        public static class AllowInertClickEvent
                extends ComponentEvent<AllowInertClickButton> {
            public AllowInertClickEvent(AllowInertClickButton source,
                    boolean fromClient) {
                super(source, fromClient);
            }
        }
    }

    public class Dialog extends Div {

        public Dialog() {
            add(new Span("A modal dialog"));
            getStyle().set("position", "fixed").set("inset", "50% 50%")
                    .set("border", "1px solid black")
                    .set("background-color", "white").set("padding", "10px")
                    .set("z-index", "1000");
        }

        public void open() {
            UI.getCurrent().addModal(this);
        }
    }
}
