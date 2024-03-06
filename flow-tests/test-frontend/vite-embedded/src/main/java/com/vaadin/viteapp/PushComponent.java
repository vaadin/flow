/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.viteapp;

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.page.Push;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.DetachEvent;
import com.vaadin.flow.component.WebComponentExporter;
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
