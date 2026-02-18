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
package com.vaadin.flow.uitest.ui.signal;

import java.util.concurrent.atomic.AtomicInteger;

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.NativeButton;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.signals.shared.SharedValueSignal;
import com.vaadin.flow.uitest.servlet.ViewTestLayout;

/**
 * View for testing SharedValueSignal.
 */
@Route(value = "com.vaadin.flow.uitest.ui.signal.SharedValueSignalView", layout = ViewTestLayout.class)
public class SharedValueSignalView extends Div {

    private final SharedValueSignal<String> SHARED_SIGNAL = new SharedValueSignal<>(
            "initial");

    private final AtomicInteger COUNTER = new AtomicInteger(1);

    public SharedValueSignalView() {
        addTestViewForWriteThroughTransactionWithRepeatableReads();
    }

    private void addTestViewForWriteThroughTransactionWithRepeatableReads() {
        Div firstReadValue = new Div();
        firstReadValue.setId("first-read-value");
        Div secondReadValue = new Div();
        secondReadValue.setId("second-read-value");
        Div secondPeekConfirmedValue = new Div();
        secondPeekConfirmedValue.setId("second-peek-confirmed-value");
        Div signalValue = new Div();
        signalValue.setId("signal-value");

        // This test view is used to verify that the shared signal
        // provides repeatable reads within a transaction, even if it is updated
        // by another thread.
        NativeButton sharedButton = new NativeButton(
                "Shared Signal Repeatable Read Button");
        sharedButton.setId("repeatable-read-button");
        sharedButton.addClickListener(e -> {
            // change signal value in another thread to simulate concurrent
            // update
            Thread.startVirtualThread(() -> {
                try {
                    Thread.sleep(50); // Simulate delay
                } catch (InterruptedException ex) {
                    throw new RuntimeException(ex);
                }
                var newValue = "updated #" + COUNTER.getAndIncrement();
                SHARED_SIGNAL.set(newValue);
            });

            String firstRead = SHARED_SIGNAL.get();
            firstReadValue.setText(firstRead);

            try {
                Thread.sleep(100); // Simulate delay
            } catch (InterruptedException ex) {
                throw new RuntimeException(ex);
            }

            String secondRead = SHARED_SIGNAL.get();
            secondReadValue.setText(secondRead);
            secondPeekConfirmedValue.setText(SHARED_SIGNAL.peekConfirmed());
        });

        NativeButton printSignalButton = new NativeButton("Print signal value");
        printSignalButton.setId("print-signal-button");
        printSignalButton.addClickListener(e -> {
            signalValue.setText("Current signal value: " + SHARED_SIGNAL.get());
        });

        add(sharedButton, printSignalButton, firstReadValue, secondReadValue,
                secondPeekConfirmedValue, signalValue);
    }
}
