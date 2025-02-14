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

import java.util.EventObject;
import java.util.concurrent.atomic.AtomicInteger;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.Key;
import com.vaadin.flow.component.Shortcuts;
import com.vaadin.flow.component.Text;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Input;
import com.vaadin.flow.component.html.NativeButton;
import com.vaadin.flow.router.Route;

@Route(value = "com.vaadin.flow.uitest.ui.DialogShortcutView")
public class DialogShortcutView extends Div {

    public static final String EVENT_LOG_ID = "event-log";
    public static final String UI_BUTTON = "ui-button";
    public static final String OPEN_BUTTON = "open-button";
    public static final String DIALOG_BUTTON = "dialog-button";
    public static final String DIALOG_CLOSE_BUTTON = "dialog-close-button";
    public static final String DIALOG_ID = "DIALOG";
    public static final String LISTEN_ON_UI_BUTTON = "listen-on-ui-button";
    public static final String LISTEN_ON_DIALOG_BUTTON = "listen-on-dialog-button";
    public static final String LISTEN_CLICK_ON_UI_BUTTON = "listen-click-on-ui-button";
    public static final String LISTEN_CLICK_ON_DIALOG_BUTTON = "listen-click-on-dialog-button";
    public static final String REUSABLE_DIALOG_BUTTON = "reusable-dialog-button";
    public static final String ALLOW_BROWSER_DEFAULT_BUTTON = "allow-browser-default";
    public static final String UI_ID = "UI-ID";
    public static final String CONTENT_ID = "CONTENT";
    public static final String KEY_STRING = "x";
    public static final Key SHORTCUT_KEY = Key.KEY_X;
    public static final int REUSABLE_DIALOG_ID = 999;

    private final AtomicInteger dialogCounter = new AtomicInteger(-1);
    private int eventCounter;
    private final Div eventLog;
    private boolean allowBrowserDefault;

    private Dialog reusedDialog;

    public DialogShortcutView() {
        eventLog = new Div(new Text("Click events and their sources:"));
        eventLog.setId(EVENT_LOG_ID);

        final NativeButton allowBrowserDefaultButton = new NativeButton(
                "Allow Browser Default", event -> {
                    allowBrowserDefault = true;
                    event.getSource().setEnabled(false);
                });
        allowBrowserDefaultButton.setId(ALLOW_BROWSER_DEFAULT_BUTTON);
        final NativeButton testButton = createButton(
                "UI level button with shortcut", this::logClickEvent);
        testButton.setId(UI_BUTTON);
        testButton.addClickShortcut(SHORTCUT_KEY);

        add(new Div(new Text("Shortcut key: " + KEY_STRING)),
                createOpenDialogButton(OPEN_BUTTON), testButton,
                allowBrowserDefaultButton, eventLog);
        setId("main-div");

        final NativeButton reusableDialogButton = new NativeButton(
                "Open reusable dialog", event -> {
                    if (reusedDialog == null) {
                        reusedDialog = new Dialog(REUSABLE_DIALOG_ID);
                    }
                    open(reusedDialog);
                    eventLog.add(new Div(new Text("Opened reusable dialog DC"
                            + dialogCounter + "-EC" + eventCounter)));
                });
        reusableDialogButton.setId(REUSABLE_DIALOG_BUTTON);
        add(reusableDialogButton);
    }

    private void logClickEvent(EventObject event) {
        eventLog.addComponentAsFirst(new Div(new Text(
                (eventCounter++) + "-" + ((Component) event.getSource()).getId()
                        .orElse("NO-SOURCE-ID"))));
    }

    private Component createOpenDialogButton(String id) {
        final NativeButton button = createButton("Open dialog", event -> {
            final Dialog dialog = new Dialog(dialogCounter.incrementAndGet());
            open(dialog);
            eventLog.add(new Div(new Text("Opened dialog DC" + dialogCounter
                    + "-EC" + eventCounter)));
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

        public final int index;
        public final Div content;

        public Dialog(int index) {
            this.index = index;
            final NativeButton testButton = createButton(
                    "Test button with shortcut",
                    DialogShortcutView.this::logClickEvent);
            testButton.setId(DIALOG_BUTTON + index);
            final NativeButton uiScopeClickShortcutButton = new NativeButton(
                    "Add button click shortcut with listenOn(UI) (default)",
                    event -> {
                        testButton.addClickShortcut(SHORTCUT_KEY);
                        event.getSource().setEnabled(false);
                    });
            uiScopeClickShortcutButton.setId(LISTEN_CLICK_ON_UI_BUTTON + index);
            final NativeButton dialogScopeClickShortcutButton = new NativeButton(
                    "Add button click shortcut with listenOn(Dialog)",
                    event -> {
                        testButton.addClickShortcut(SHORTCUT_KEY)
                                .listenOn(Dialog.this);
                        event.getSource().setEnabled(false);
                    });
            dialogScopeClickShortcutButton
                    .setId(LISTEN_CLICK_ON_DIALOG_BUTTON + index);
            final NativeButton uiScopeShortcutButton = new NativeButton(
                    "Add shortcut with listenOn(UI) (default)", event -> {
                        Shortcuts
                                .addShortcutListener(Dialog.this,
                                        DialogShortcutView.this::logClickEvent,
                                        SHORTCUT_KEY)
                                .setBrowserDefaultAllowed(allowBrowserDefault);
                        event.getSource().setEnabled(false);
                    });
            uiScopeShortcutButton.setId(LISTEN_ON_UI_BUTTON + index);
            final NativeButton dialogScopeShortcutButton = new NativeButton(
                    "Add shortcut with listenOn(Dialog)", event -> {
                        Shortcuts
                                .addShortcutListener(Dialog.this,
                                        DialogShortcutView.this::logClickEvent,
                                        SHORTCUT_KEY)
                                .listenOn(Dialog.this)
                                .setBrowserDefaultAllowed(allowBrowserDefault);
                        event.getSource().setEnabled(false);
                    });
            dialogScopeShortcutButton.setId(LISTEN_ON_DIALOG_BUTTON + index);

            final Component closeButton = createButton("Close",
                    event -> close(this));
            closeButton.setId(DIALOG_CLOSE_BUTTON + index);
            content = new Div(new Input(), new Div(), closeButton, new Div(),
                    uiScopeShortcutButton, dialogScopeShortcutButton, new Div(),
                    uiScopeClickShortcutButton, dialogScopeClickShortcutButton,
                    new Div(), testButton);
            content.getStyle().set("border", "1px solid black").set("margin",
                    "10px");
            content.setId(CONTENT_ID + index);
            add(content);

            setId(DIALOG_ID + index);
        }
    }

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        super.onAttach(attachEvent);
        attachEvent.getUI().setId(UI_ID);
    }

    public void open(Dialog dialog) {
        add(dialog);
    }

    public void close(Dialog dialog) {
        remove(dialog);
        eventLog.add(new Div(new Text("Dialog " + dialog.index + " closed")));
    }
}
