/*
 * Copyright 2000-2025 Vaadin Ltd.
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

import java.util.List;
import java.util.Map;

import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.Key;
import com.vaadin.flow.component.Text;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Input;
import com.vaadin.flow.component.html.NativeButton;
import com.vaadin.flow.router.BeforeEvent;
import com.vaadin.flow.router.HasUrlParameter;
import com.vaadin.flow.router.Location;
import com.vaadin.flow.router.OptionalParameter;
import com.vaadin.flow.router.Route;

@Route(value = "com.vaadin.flow.uitest.ui.ModalDialogView")
public class ModalDialogView extends Div implements HasUrlParameter<String> {

    public static final String EVENT_LOG = "event-log";
    public static final String UI_BUTTON = "ui-button";
    public static final String OPEN_MODELESS_BUTTON = "modeless-dialog-button";
    public static final String OPEN_MODAL_BUTTON = "modal-dialog-button";
    public static final String DIALOG_BUTTON = "dialog-button";
    public static final String DIALOG_CLOSE_BUTTON = "dialog-close-button";
    public static final String DIALOG = "DIALOG";
    public static final String LISTEN_ON_UI_BUTTON = "listen-on-ui-button";
    public static final String LISTEN_ON_DIALOG_BUTTON = "listen-on-dialog-button";
    public static final Key SHORTCUT_KEY = Key.KEY_X;
    private int eventCounter;
    private final Div eventLog;

    public ModalDialogView() {
        eventLog = new Div(new Text("Click events and their sources:"));
        eventLog.setId(EVENT_LOG);

        final NativeButton testButton = createButton("Key-X shortcut",
                this::logClickEvent);
        testButton.setId(UI_BUTTON);
        testButton.addClickShortcut(SHORTCUT_KEY);

        add(createOpenDialogButton(true, OPEN_MODAL_BUTTON),
                createOpenDialogButton(false, OPEN_MODELESS_BUTTON), testButton,
                eventLog);
        setId("main-div");
    }

    @Override
    public void setParameter(BeforeEvent event,
            @OptionalParameter String parameter) {
        Location location = event.getLocation();
        Map<String, List<String>> queryParameters = location
                .getQueryParameters().getParameters();
        if (queryParameters.containsKey("open_dialog")) {
            boolean modal = queryParameters.get("open_dialog")
                    .contains("modal");
            final Dialog dialog = new Dialog(modal);
            dialog.open();
        }
    }

    private void logClickEvent(ClickEvent<?> event) {
        eventLog.addComponentAsFirst(new Div(new Text((eventCounter++) + "-"
                + event.getSource().getId().orElse("NO-SOURCE-ID"))));
    }

    private Component createOpenDialogButton(boolean modal, String id) {
        final NativeButton button = createButton(
                "Open " + (modal ? "modal" : "non-modal") + " dialog",
                event -> {
                    final Dialog dialog = new Dialog(modal);
                    dialog.open();
                });
        button.setId(id);
        return button;
    }

    private NativeButton createButton(String caption,
            ComponentEventListener<ClickEvent<NativeButton>> listener) {
        final NativeButton button = new NativeButton();
        button.setText(caption);
        button.addClickListener(listener);
        button.getStyle().set("border", "1px solid black");
        button.setWidth("100px");
        return button;
    }

    public class Dialog extends Div {
        private boolean modal;

        public Dialog(boolean modal) {
            this.modal = modal;

            final NativeButton testButton = createButton(
                    "Test button with enter shortcut",
                    ModalDialogView.this::logClickEvent);
            testButton.setId(DIALOG_BUTTON);
            final NativeButton uiScopeShortcutButton = new NativeButton(
                    "Add shortcut with listenOn(UI)", event -> {
                        testButton.addClickShortcut(SHORTCUT_KEY);
                        event.getSource().setEnabled(false);
                    });
            uiScopeShortcutButton.setId(LISTEN_ON_UI_BUTTON);
            final NativeButton dialogScopeShortcutButton = new NativeButton(
                    "Add shortcut with listenOn(Dialog)", event -> {
                        testButton.addClickShortcut(SHORTCUT_KEY)
                                .listenOn(Dialog.this);
                        event.getSource().setEnabled(false);
                    });
            dialogScopeShortcutButton.setId(LISTEN_ON_DIALOG_BUTTON);

            final Component closeButton = createButton("Close",
                    event -> close());
            closeButton.setId(DIALOG_CLOSE_BUTTON);
            add(new Text("A " + (modal ? "modal" : "modeless") + " dialog"),
                    new Input(), new Div(), closeButton, uiScopeShortcutButton,
                    dialogScopeShortcutButton, new Div(), testButton);

            getUI().ifPresent(ui -> {
                ui.setChildComponentModal(this, modal);
            });
            getStyle().set("position", "fixed").set("inset", "50% 50%")
                    .set("border", "1px solid black");
            setId(DIALOG);
        }

        public void open() {
            final UI ui = UI.getCurrent();
            if (modal) {
                ui.addModal(this);
            } else {
                ui.add(this);
            }
        }

        public void close() {
            final UI ui = ModalDialogView.this.getUI().get();
            ui.remove(this);
        }
    }
}
