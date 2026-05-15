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

import com.vaadin.flow.component.html.NativeButton;
import com.vaadin.flow.component.trigger.ClickTrigger;
import com.vaadin.flow.component.trigger.ClipboardCopyAction;
import com.vaadin.flow.component.trigger.SignalOutput;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.signals.local.ValueSignal;
import com.vaadin.flow.uitest.servlet.ViewTestLayout;

/**
 * Wires a {@link ClickTrigger} on a button to a {@link ClipboardCopyAction}
 * reading from a {@link SignalOutput} backed by a server-side
 * {@link ValueSignal}. A second "Update" button mutates the signal so the IT
 * can assert the snapshot re-syncs and the clipboard receives the new value on
 * the next click.
 */
@Route(value = "com.vaadin.flow.uitest.ui.TriggerSignalOutputView", layout = ViewTestLayout.class)
public class TriggerSignalOutputView extends AbstractDivView {

    @Override
    protected void onShow() {
        ValueSignal<String> message = new ValueSignal<>("first");

        NativeButton copy = new NativeButton("Copy signal");
        copy.setId("copy");

        NativeButton update = new NativeButton("Update signal",
                e -> message.set("second"));
        update.setId("update");

        add(copy, update);

        new ClickTrigger(copy).triggers(new ClipboardCopyAction(
                new SignalOutput<>(String.class, message)));
    }
}
