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
import java.io.Serializable;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.TreeMap;
import java.util.function.Consumer;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openjdk.jol.info.GraphLayout;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;

import com.vaadin.flow.component.AbstractSinglePropertyField;
import com.vaadin.flow.component.ClickNotifier;
import com.vaadin.flow.component.ClientCallable;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.HasLabel;
import com.vaadin.flow.component.HasPlaceholder;
import com.vaadin.flow.component.HasText;
import com.vaadin.flow.component.HasTheme;
import com.vaadin.flow.component.HasValidation;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Input;
import com.vaadin.flow.component.html.NativeButton;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.data.binder.Binder;
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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;

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
 * Each scenario writes a report to {@code target/memory-footprint/<id>.json}
 * with its title, its description and its metrics, each with an id, a title, a
 * unit, a value and optionally a breakdown of the value per class.
 * {@code scripts/memoryFootprint.js} compares two sets of reports without
 * knowing the scenarios or the metrics, so a scenario or a metric is added here
 * only.
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
        ApplicationConfiguration applicationConfiguration = mock(
                ApplicationConfiguration.class, withSettings().stubOnly());
        when(applicationConfiguration.isProductionMode()).thenReturn(true);
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
        measure("empty-ui", "Empty UI",
                "A UI with no components, which is the cost of every UI.",
                ui -> {
                });
    }

    @Test
    void uiWith200ComponentRows() throws IOException {
        measure("ui-with-200-component-rows", "UI with 200 component rows",
                "A UI with 200 rows, each a div with a class name and a style,"
                        + " holding a span, an input with a value change"
                        + " listener and a button with a click listener.",
                UIMemoryFootprintTest::addRows);
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

    @Test
    void uiWith200FormRows() throws IOException {
        measure("ui-with-200-form-rows", "UI with 200 form rows",
                "A UI with 200 rows, each a layout with a theme and several"
                        + " styles, holding a text field and a combo box"
                        + " bound to a bean with a binder and a button with"
                        + " a click listener. The fields are web components"
                        + " with several properties and a synchronized"
                        + " value, and the combo box has client callable"
                        + " methods, as Vaadin components that load data"
                        + " do.",
                UIMemoryFootprintTest::addFormRows);
    }

    @Test
    void uiWith200FormRowsAddedInLatestRoundTrip() throws IOException {
        measure("ui-with-200-form-rows-added-in-latest-round-trip",
                "UI with 200 form rows added in the latest round trip",
                "The UI with 200 form rows, with the rows added in the latest"
                        + " round trip, as when navigating to a view, so that"
                        + " the response kept for resending holds all of"
                        + " them.",
                ui -> {
                }, UIMemoryFootprintTest::addFormRows);
    }

    private static void addFormRows(UI ui) {
        Div list = new Div();
        for (int i = 0; i < 200; i++) {
            Div row = new Div();
            row.getElement().setAttribute("theme", "spacing padding");
            row.getStyle().set("display", "flex").set("gap", "1em")
                    .set("align-items", "baseline");
            TextField name = new TextField();
            name.setLabel("Name");
            name.setPlaceholder("First and last name");
            name.addThemeName("small");
            ComboBox role = new ComboBox();
            role.setLabel("Role");
            role.setPlaceholder("Select a role");
            Binder<Person> binder = new Binder<>();
            binder.forField(name).asRequired("Name is required")
                    .bind(Person::getName, Person::setName);
            binder.forField(role).bind(Person::getRole, Person::setRole);
            binder.setBean(new Person("Person " + i, "User"));
            Button save = new Button("Save");
            save.addThemeName("primary");
            save.addClickListener(event -> binder.validate());
            row.add(name, role, save);
            list.add(row);
        }
        ui.add(list);
    }

    /**
     * Opens UIs with the given view, measures them and writes the report of the
     * scenario to {@code target/memory-footprint/<id>.json}.
     *
     * @param id
     *            identifies the scenario between builds, so it should stay the
     *            same as long as the scenario measures the same thing
     * @param title
     *            names the scenario in the pull request comment
     * @param description
     *            describes what the view of the scenario contains
     * @param view
     *            adds the content of the view to a UI
     */
    private void measure(String id, String title, String description,
            Consumer<UI> view) throws IOException {
        // The response kept for resending is the one of the latest round
        // trip, which is usually a small update rather than the first render
        measure(id, title, description, view,
                ui -> ui.getElement().setAttribute("data-round-trip", "1"));
    }

    /**
     * Like {@link #measure(String, String, String, Consumer)}, with the given
     * change to the UI in the latest round trip.
     *
     * @param update
     *            changes the UI in the latest round trip, whose response is the
     *            one kept for resending
     */
    private void measure(String id, String title, String description,
            Consumer<UI> view, Consumer<UI> update) throws IOException {
        UI first = open(view, update);
        long serializedSize = serializedSize(session);
        // Opening and serializing a UI fills caches shared by all UIs, such
        // as the reflection data of classes, so they are filled before
        // measuring the UI opened after this one
        UI second = open(view, update);
        serializedSize(session);
        // Walking the graph fills the reflection data of the classes it
        // walks, so the graph is walked once before measuring it
        GraphLayout.parseInstance(session, first, second);
        // Totals rather than GraphLayout.subtract, which matches objects by
        // address and would be thrown off by a garbage collection in between
        GraphLayout before = GraphLayout.parseInstance(session, first, second);

        UI third = open(view, update);
        GraphLayout after = GraphLayout.parseInstance(session, first, second,
                third);

        long retainedSize = after.totalSize() - before.totalSize();
        assertTrue(retainedSize > 0, "A UI should retain some memory");

        ObjectNode report = JacksonUtils.createObjectNode();
        report.put("title", title);
        report.put("description", description);
        ArrayNode metrics = report.putArray("metrics");
        ObjectNode heap = metrics.addObject().put("id", "heap-retained-per-ui")
                .put("title", "Heap retained per UI").put("unit", "bytes")
                .put("value", retainedSize);
        ObjectNode classes = heap.putObject("breakdown");
        classDifference(before, after).forEach((name, difference) -> classes
                .putObject(name).put("count", difference[0])
                .put("bytes", difference[1]));
        metrics.addObject().put("id", "serialized-session-with-one-ui")
                .put("title", "Serialized session with one UI")
                .put("unit", "bytes").put("value", serializedSize);

        Path file = Path.of("target", "memory-footprint", id + ".json");
        Files.createDirectories(file.getParent());
        Files.writeString(file, report.toPrettyString());
    }

    private UI open(Consumer<UI> view, Consumer<UI> update) {
        MockUI ui = new MockUI(session);
        ui.doInit(mock(VaadinRequest.class), session.getNextUIid(), "ROOT");
        session.addUI(ui);
        view.accept(ui);
        respond(ui);
        update.accept(ui);
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

    /**
     * A field like the Vaadin text field, which keeps its value and its other
     * state in properties of a web component.
     */
    @Tag("vaadin-text-field")
    private static class TextField
            extends AbstractSinglePropertyField<TextField, String>
            implements HasLabel, HasPlaceholder, HasTheme, HasValidation {

        TextField() {
            super("value", "", false);
            getElement().setProperty("clearButtonVisible", true);
            getElement().setProperty("autoselect", false);
            setManualValidation(true);
        }

        @Override
        public void setErrorMessage(String errorMessage) {
            getElement().setProperty("errorMessage", errorMessage);
        }

        @Override
        public String getErrorMessage() {
            return getElement().getProperty("errorMessage");
        }

        @Override
        public void setInvalid(boolean invalid) {
            getElement().setProperty("invalid", invalid);
        }

        @Override
        public boolean isInvalid() {
            return getElement().getProperty("invalid", false);
        }
    }

    /**
     * A field like the Vaadin combo box, which fetches its items through client
     * callable methods.
     */
    @Tag("vaadin-combo-box")
    private static class ComboBox extends TextField {

        ComboBox() {
            getElement().setProperty("pageSize", 50);
            getElement().setProperty("itemLabelPath", "label");
            getElement().setProperty("itemValuePath", "key");
        }

        @ClientCallable
        private void setRequestedRange(int start, int length, String filter) {
            // Only declared for the handlers it adds to the component, the
            // scenario never calls it
        }

        @ClientCallable
        private void resetDataCommunicator() {
            // Only declared for the handlers it adds to the component, the
            // scenario never calls it
        }
    }

    @Tag("vaadin-button")
    private static class Button extends Component
            implements ClickNotifier<Button>, HasText, HasTheme {

        Button(String text) {
            setText(text);
            getElement().setAttribute("tabindex", "0");
        }
    }

    private static class Person implements Serializable {
        private String name;
        private String role;

        Person(String name, String role) {
            this.name = name;
            this.role = role;
        }

        String getName() {
            return name;
        }

        void setName(String name) {
            this.name = name;
        }

        String getRole() {
            return role;
        }

        void setRole(String role) {
            this.role = role;
        }
    }
}
