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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.TreeMap;
import java.util.function.Consumer;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.openjdk.jol.info.GraphLayout;
import tools.jackson.databind.node.ObjectNode;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Input;
import com.vaadin.flow.component.html.NativeButton;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.di.Lookup;
import com.vaadin.flow.internal.CurrentInstance;
import com.vaadin.flow.internal.JacksonUtils;
import com.vaadin.flow.router.DefaultRoutePathProvider;
import com.vaadin.flow.router.RoutePathProvider;
import com.vaadin.flow.server.MockServletConfig;
import com.vaadin.flow.server.MockVaadinServletService;
import com.vaadin.flow.server.VaadinRequest;
import com.vaadin.flow.server.VaadinSession;
import com.vaadin.flow.server.communication.UidlWriter;
import com.vaadin.flow.server.startup.ApplicationConfiguration;
import com.vaadin.tests.util.AlwaysLockedVaadinSession;
import com.vaadin.tests.util.MockDeploymentConfiguration;
import com.vaadin.tests.util.MockUI;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Measures the memory a UI uses, so that changes to it can be compared between
 * two builds. The measurements are exact and the same on every run of the same
 * JVM, unlike ones based on the used heap after a garbage collection.
 * <p>
 * Each scenario measures:
 * <ul>
 * <li>the bytes a UI retains on the heap: the size of the object graph that a
 * UI adds to a session that already has identical ones. Objects shared by all
 * UIs, such as the session, the service and anything cached per class, are
 * reachable from the other UIs as well, so they are not counted.</li>
 * <li>the size of the session serialized with one UI open, which is what is
 * stored or replicated when sessions are persisted.</li>
 * </ul>
 * The results are written to {@code target/memory-footprint/<scenario>.json}
 * together with the retained instances and bytes per class, and
 * {@code scripts/memoryFootprint.js} compares two sets of them.
 */
class UIMemoryFootprintTest {

    static {
        // JOL can not read the field offsets of records with sun.misc.Unsafe,
        // and the JDK has records among the objects a UI refers to
        System.setProperty("jol.magicFieldOffset", "true");
    }

    private VaadinSession session;

    @BeforeEach
    void setUp() throws Exception {
        // Mocks that record their invocations would grow with every UI, so
        // the service is set up with stub-only mocks
        MockDeploymentConfiguration deploymentConfiguration = new MockDeploymentConfiguration();
        // Sessions are serialized with their UIs only in production mode
        deploymentConfiguration.setProductionMode(true);
        MockVaadinServletService service = new MockVaadinServletService(
                deploymentConfiguration, false);
        service.getServlet().init(new MockServletConfig());
        ApplicationConfiguration applicationConfiguration = Mockito.mock(
                ApplicationConfiguration.class,
                Mockito.withSettings().stubOnly());
        Mockito.when(applicationConfiguration.isProductionMode())
                .thenReturn(true);
        service.getContext().setAttribute(ApplicationConfiguration.class,
                applicationConfiguration);
        service.getContext().setAttribute(Lookup.class, Lookup
                .of(new DefaultRoutePathProvider(), RoutePathProvider.class));
        service.init();
        session = new AlwaysLockedVaadinSession(service);
        VaadinSession.setCurrent(session);
    }

    @AfterEach
    void tearDown() {
        session.unlock();
        CurrentInstance.clearAll();
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

    private void measure(String scenario, Consumer<UI> view)
            throws IOException {
        UI first = open(view);
        long serializedSize = serializedSize(session);
        // Opening and serializing a UI fills caches shared by all UIs, such
        // as the reflection data of classes, so they are filled before
        // measuring the UI opened after this one
        UI second = open(view);
        serializedSize(session);
        // Walking the graph fills the reflection data of the classes it
        // walks, so the graph is walked once before measuring it
        GraphLayout.parseInstance(session, first, second);
        // Totals rather than GraphLayout.subtract, which matches objects by
        // address and would be thrown off by a garbage collection in between
        GraphLayout before = GraphLayout.parseInstance(session, first, second);

        UI third = open(view);
        GraphLayout after = GraphLayout.parseInstance(session, first, second,
                third);

        long retainedSize = after.totalSize() - before.totalSize();
        assertTrue(retainedSize > 0, "A UI should retain some memory");

        ObjectNode report = JacksonUtils.createObjectNode();
        report.put("retainedBytesPerUI", retainedSize);
        report.put("serializedSessionBytes", serializedSize);
        ObjectNode classes = report.putObject("classes");
        classDifference(before, after).forEach((name, difference) -> classes
                .putObject(name).put("count", difference[0])
                .put("bytes", difference[1]));

        Path file = Path.of("target", "memory-footprint", scenario + ".json");
        Files.createDirectories(file.getParent());
        Files.writeString(file, report.toPrettyString());
    }

    private UI open(Consumer<UI> view) {
        MockUI ui = new MockUI(session);
        ui.doInit(Mockito.mock(VaadinRequest.class), session.getNextUIid(),
                "ROOT");
        session.addUI(ui);
        view.accept(ui);
        respond(ui);
        // The response kept for resending is the one of the latest round
        // trip, which is usually a small update rather than the first render
        ui.getElement().setAttribute("data-round-trip", "1");
        respond(ui);
        return ui;
    }

    private static void respond(UI ui) {
        // Like UidlRequestHandler, which keeps the response for resending
        String response = new UidlWriter().createUidl(ui, false, false)
                .toString();
        ui.getInternals().setLastRequestResponse(response);
    }

    private static long serializedSize(VaadinSession session)
            throws IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (ObjectOutputStream out = new ObjectOutputStream(bytes)) {
            out.writeObject(session);
        }
        return bytes.size();
    }

    /**
     * Instances and bytes per class that the measured UI added, keyed by class
     * name. Lambda classes are named after the class that declares them, since
     * the rest of their name changes between builds.
     */
    private static TreeMap<String, long[]> classDifference(GraphLayout before,
            GraphLayout after) {
        TreeMap<String, long[]> difference = new TreeMap<>();
        for (Class<?> type : after.getClasses()) {
            long count = after.getClassCounts().count(type)
                    - before.getClassCounts().count(type);
            long bytes = after.getClassSizes().count(type)
                    - before.getClassSizes().count(type);
            if (count != 0 || bytes != 0) {
                long[] total = difference.computeIfAbsent(type.getName()
                        .replaceFirst("\\$\\$Lambda.*", "\\$\\$Lambda"),
                        name -> new long[2]);
                total[0] += count;
                total[1] += bytes;
            }
        }
        return difference;
    }
}
