/*
 * Copyright 2000-2019 Vaadin Ltd.
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
 *
 */

package com.vaadin.flow.uitest.ui;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.DetachEvent;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.html.NativeButton;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.page.Push;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.shared.ui.Transport;

/**
 * Test for reproducing the bug https://github.com/vaadin/flow/issues/4660
 *
 * @since 1.4
 */
@Route("com.vaadin.flow.uitest.ui.LongPollingMultipleThreadsView")
@Push(transport = Transport.LONG_POLLING)
public class LongPollingMultipleThreadsView extends AbstractDivView {

    private final ScheduledExecutorService executor = Executors
            .newScheduledThreadPool(10);

    private final List<Long> itemRegistry = new LinkedList<>();

    private final Span label = new Span();

    private UI ui;

    public LongPollingMultipleThreadsView() {
        this.setId("push-update");
        NativeButton startButton = createButton("start", "start-button",
                event -> this.start());
        add(startButton, label);
    }

    private void start() {
        synchronized (itemRegistry) {
            itemRegistry.clear();
        }

        updateDiv(new ArrayList<>());

        for (int i = 0; i < 5; ++i) {
            executor.submit(this::doWork);
        }
    }

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        updateDiv(new ArrayList<>());
        ui = attachEvent.getUI();
    }

    private void doWork() {
        try {
            Thread.sleep((int) (Math.random() * 10));
        } catch (InterruptedException ex) {
            throw new RuntimeException(ex);
        }

        // Simulate some new piece of data coming in on a background thread and
        // getting put into the display.
        List<Long> copy;
        synchronized (itemRegistry) {
            itemRegistry.add(System.currentTimeMillis());

            // Copy the list so we're not accessing the shared registry
            // concurrently.
            copy = new ArrayList<>(itemRegistry);
        }

        ui.access(() -> updateDiv(copy));

    }

    @Override
    protected void onDetach(DetachEvent detachEvent) {
        executor.shutdownNow();
    }

    private void updateDiv(List<Long> items) {
        label.setText(items.toString());
    }
}
