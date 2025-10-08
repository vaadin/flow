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
package com.vaadin.viteapp;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.DetachEvent;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.WebComponentExporter;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.page.Push;
import com.vaadin.flow.component.webcomponent.WebComponent;

public class PushComponent extends Div {
    @Push()
    public static class Exporter extends WebComponentExporter<PushComponent> {
        public Exporter() {
            super("push-component");
        }

        @Override
        protected void configureInstance(
                WebComponent<PushComponent> webComponent,
                PushComponent component) {
        }
    }

    private static final int DELAY = 100;
    private static final int MAX_UPDATE = 50;

    private AtomicInteger count = new AtomicInteger();

    private final ScheduledExecutorService service = Executors
            .newScheduledThreadPool(1);

    protected void onAttach(AttachEvent attachEvent) {
        update();

        scheduleUpdate(attachEvent.getUI());
    }

    private void scheduleUpdate(final UI ui) {
        service.schedule(() -> {
            ui.access(this::update);
            if (count.getAndIncrement() < MAX_UPDATE) {
                scheduleUpdate(ui);
            } else {
                service.shutdown();
            }
        }, DELAY, TimeUnit.MILLISECONDS);
    }

    @Override
    protected void onDetach(DetachEvent detachEvent) {
        service.shutdownNow();
    }

    private void update() {
        setText(String.valueOf(count.get()));
    }
}
