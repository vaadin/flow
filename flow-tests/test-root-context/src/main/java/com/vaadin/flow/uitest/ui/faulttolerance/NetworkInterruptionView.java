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
package com.vaadin.flow.uitest.ui.faulttolerance;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.NativeButton;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.router.AfterNavigationEvent;
import com.vaadin.flow.router.AfterNavigationObserver;
import com.vaadin.flow.router.Route;

@Route("com.vaadin.flow.uitest.ui.faulttolerance.NetworkInterruptionView")
public class NetworkInterruptionView extends Div
        implements AfterNavigationObserver {

    public static final String INCREMENT_BUTTON_ID = "incrementCounter";
    public static final String INCREMENT_STOP_PROXY_BUTTON_ID = "incrementCounterStopProxy";
    public static final String COUNTER_ID = "counter";
    private final NativeButton incrementAndStopProxyButton;

    private int clientCounter = 0;
    private String monitorFile;

    public NetworkInterruptionView() {
        Span counter = new Span("0");
        counter.setId(COUNTER_ID);
        NativeButton incrementButton = new NativeButton("Increment", e -> {
            clientCounter++;
            counter.setText(clientCounter + "");
        });
        incrementButton.setId(INCREMENT_BUTTON_ID);
        incrementAndStopProxyButton = new NativeButton("Increment (stop proxy)",
                e -> {
                    clientCounter++;
                    counter.setText(clientCounter + "");
                    BeforeOutputStreamActionFilter.beforeGettingOutputStream(
                            this::stopProxyConnection);
                });
        incrementAndStopProxyButton.setId(INCREMENT_STOP_PROXY_BUTTON_ID);
        add(incrementButton, incrementAndStopProxyButton, counter);
    }

    @Override
    public void afterNavigation(AfterNavigationEvent event) {
        monitorFile = event.getLocation().getQueryParameters()
                .getSingleParameter("proxyMonitorFile").orElse(null);
        if (monitorFile == null) {
            remove(incrementAndStopProxyButton);
        }
    }

    private void stopProxyConnection() {
        try {
            Files.writeString(Paths.get(monitorFile), "stop",
                    StandardOpenOption.TRUNCATE_EXISTING);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        // wait for proxy disconnection
        try {
            Thread.sleep(200);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
        }
    }
}
