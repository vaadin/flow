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
package com.vaadin.flow.memory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.Consumer;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.openjdk.jol.info.GraphLayout;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Input;
import com.vaadin.flow.component.html.NativeButton;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.server.VaadinSession;
import com.vaadin.flow.server.communication.UidlWriter;
import com.vaadin.tests.util.MockUI;

/**
 * Measures the memory a UI retains, so that changes to it can be compared
 * between two builds.
 * <p>
 * The retained size is the size of the object graph that a second, identical UI
 * adds to a session that already has one. Objects shared by all UIs, such as
 * the session, the service and anything cached per class, are reachable from
 * the first UI as well, so they are not counted. The result is exact and the
 * same on every run of the same JVM, unlike measurements based on the used heap
 * after a garbage collection.
 * <p>
 * Each scenario writes the bytes per UI and a histogram of the retained objects
 * to {@code target/memory-footprint/<scenario>.txt}.
 */
class UIMemoryFootprintTest {

    static {
        // JOL can not read the field offsets of records with sun.misc.Unsafe,
        // and the JDK has records among the objects a UI refers to
        System.setProperty("jol.magicFieldOffset", "true");
    }

    @Test
    void emptyUI() throws IOException {
        measure("emptyUI", ui -> {
        });
    }

    @Test
    void rowsView() throws IOException {
        measure("rowsView", UIMemoryFootprintTest::addRows);
    }

    private static void addRows(UI ui) {
        Div list = new Div();
        for (int i = 0; i < 200; i++) {
            Div row = new Div();
            row.addClassName("row");
            row.getStyle().set("display", "flex");
            Input input = new Input();
            input.setValue("value " + i);
            input.addValueChangeListener(event -> {
            });
            row.add(new Span("Row " + i), input,
                    new NativeButton("Edit", event -> {
                    }));
            list.add(row);
        }
        ui.add(list);
    }

    private static void measure(String scenario, Consumer<UI> view)
            throws IOException {
        MockUI first = MockUI.createUI();
        VaadinSession session = first.getSession();
        open(first, view);
        GraphLayout oneUI = GraphLayout.parseInstance(session, first);

        MockUI second = new MockUI(session);
        open(second, view);
        GraphLayout perUI = GraphLayout.parseInstance(session, first, second)
                .subtract(oneUI);

        Assertions.assertTrue(perUI.totalSize() > 0);
        Path report = Path.of("target", "memory-footprint", scenario + ".txt");
        Files.createDirectories(report.getParent());
        Files.writeString(report, "bytes per UI: " + perUI.totalSize() + "\n"
                + perUI.toFootprint());
    }

    private static void open(UI ui, Consumer<UI> view) {
        view.accept(ui);
        respond(ui);
        // The response kept for resending is the one of the latest round
        // trip, which is usually a small update rather than the first render
        ui.getElement().setAttribute("data-round-trip", "1");
        respond(ui);
    }

    private static void respond(UI ui) {
        // Like UidlRequestHandler, which keeps the response for resending
        String response = new UidlWriter().createUidl(ui, false, false)
                .toString();
        ui.getInternals().setLastRequestResponse(response);
    }
}
