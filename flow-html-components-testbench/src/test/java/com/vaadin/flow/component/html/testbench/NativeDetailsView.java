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
package com.vaadin.flow.component.html.testbench;

import java.util.concurrent.atomic.AtomicInteger;

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.NativeButton;
import com.vaadin.flow.component.html.NativeDetails;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.router.AfterNavigationEvent;
import com.vaadin.flow.router.AfterNavigationObserver;
import com.vaadin.flow.router.Route;

@Route("Details")
public class NativeDetailsView extends Div implements AfterNavigationObserver {

    private final NativeDetails details;

    public NativeDetailsView() {
        AtomicInteger eventCounter = new AtomicInteger(0);

        Div log = new Div();
        log.setId("log");

        details = new NativeDetails("summary", new Paragraph("content"));
        details.setId("details");
        details.addToggleListener(e -> {
            log.setText("Toggle event number '" + eventCounter.incrementAndGet()
                    + "' is '" + e.isOpened() + "'");
        });

        NativeButton button = new NativeButton("open or close summary");
        button.setId("btn");
        button.addClickListener(e -> {
            // reverts the current details' open state
            details.setOpen(!details.isOpen());
        });
        add(log, button, details);
    }

    @Override
    public void afterNavigation(AfterNavigationEvent event) {
        details.setOpen(event.getLocation().getPath().endsWith("open"));
    }
}
