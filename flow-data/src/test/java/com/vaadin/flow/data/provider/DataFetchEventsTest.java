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
package com.vaadin.flow.data.provider;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import tools.jackson.databind.JsonNode;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.data.provider.hierarchy.HierarchicalDataCommunicator;
import com.vaadin.flow.data.provider.hierarchy.TreeData;
import com.vaadin.flow.data.provider.hierarchy.TreeDataProvider;
import com.vaadin.flow.server.MockVaadinServletService;
import com.vaadin.flow.server.VaadinService;
import com.vaadin.flow.server.VaadinServiceEventBus;
import com.vaadin.flow.server.VaadinSession;
import com.vaadin.flow.server.data.DataCountEndedEvent;
import com.vaadin.flow.server.data.DataCountFailedEvent;
import com.vaadin.flow.server.data.DataCountStartedEvent;
import com.vaadin.flow.server.data.DataFetchEndedEvent;
import com.vaadin.flow.server.data.DataFetchFailedEvent;
import com.vaadin.flow.server.data.DataFetchStartedEvent;
import com.vaadin.flow.shared.Registration;
import com.vaadin.flow.shared.communication.PushMode;
import com.vaadin.tests.util.AlwaysLockedVaadinSession;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifies that data provider count and fetch queries are reported on the
 * service event bus, for both flat and hierarchical data communicators.
 */
class DataFetchEventsTest {

    @Tag("test-list")
    private static class TestComponent extends Component {
    }

    /**
     * One reported query, flattened so assertions read as the timeline a
     * listener would see.
     */
    private record Recorded(String kind, Component component, int offset,
            int limit, boolean filtered, int result) {
    }

    /**
     * Records the six query events in the order they arrive.
     */
    private static final class Recorder {
        private final List<Recorded> events = new ArrayList<>();
        private final List<Throwable> failures = new ArrayList<>();

        Registration register(VaadinServiceEventBus bus) {
            List<Registration> registrations = List.of(
                    bus.addListener(DataCountStartedEvent.class,
                            event -> events.add(new Recorded("count-started",
                                    event.getComponent().orElse(null), -1, -1,
                                    event.isFiltered(), -1))),
                    bus.addListener(DataCountFailedEvent.class, event -> {
                        failures.add(event.getError());
                        events.add(new Recorded("count-failed",
                                event.getComponent().orElse(null), -1, -1,
                                event.isFiltered(), -1));
                    }),
                    bus.addListener(DataCountEndedEvent.class,
                            event -> events.add(new Recorded("count-ended",
                                    event.getComponent().orElse(null), -1, -1,
                                    event.isFiltered(), event.getCount()))),
                    bus.addListener(DataFetchStartedEvent.class,
                            event -> events.add(new Recorded("fetch-started",
                                    event.getComponent().orElse(null),
                                    event.getOffset(), event.getLimit(),
                                    event.isFiltered(), -1))),
                    bus.addListener(DataFetchFailedEvent.class, event -> {
                        failures.add(event.getError());
                        events.add(new Recorded("fetch-failed",
                                event.getComponent().orElse(null),
                                event.getOffset(), event.getLimit(),
                                event.isFiltered(), -1));
                    }),
                    bus.addListener(DataFetchEndedEvent.class,
                            event -> events.add(new Recorded("fetch-ended",
                                    event.getComponent().orElse(null),
                                    event.getOffset(), event.getLimit(),
                                    event.isFiltered(),
                                    event.getRowsReturned()))));
            return () -> registrations.forEach(Registration::remove);
        }

        List<Recorded> of(String kind) {
            return events.stream().filter(e -> e.kind().equals(kind)).toList();
        }

        Recorded first(String kind) {
            List<Recorded> matching = of(kind);
            assertFalse(matching.isEmpty(), "no " + kind + " event recorded");
            return matching.get(0);
        }
    }

    private MockVaadinServletService service;
    private UI ui;
    private TestComponent component;
    private Recorder listener;
    private Registration registration;

    @BeforeEach
    void init() {
        service = new MockVaadinServletService();
        VaadinSession session = new AlwaysLockedVaadinSession(service);
        VaadinSession.setCurrent(session);
        ui = new DataCommunicatorTest.MockUI(session);
        component = new TestComponent();
        ui.add(component);

        listener = new Recorder();
        registration = listener.register(service.getEventBus());
    }

    @AfterEach
    void tearDown() {
        UI.setCurrent(null);
        VaadinSession.setCurrent(null);
    }

    // ---------- flat ----------

    @Test
    void flatCommunicator_reportsCountAndFetch() {
        DataCommunicator<String> communicator = flatCommunicator();
        communicator.setDataProvider(itemProvider(100), null);
        communicator.setViewportRange(0, 50);
        flush();

        Recorded count = listener.first("count-ended");
        assertEquals(100, count.result(), "count query result");
        assertFalse(count.filtered(), "no filter was set");
        assertSame(component, count.component(),
                "the query is attributed to the owning component");

        Recorded fetch = listener.first("fetch-ended");
        assertEquals(0, fetch.offset());
        assertEquals(50, fetch.limit());
        assertEquals(50, fetch.result(), "rows actually returned");
        assertSame(component, fetch.component());
    }

    @Test
    void flatCommunicator_startedPrecedesEnded() {
        DataCommunicator<String> communicator = flatCommunicator();
        communicator.setDataProvider(itemProvider(10), null);
        communicator.setViewportRange(0, 10);
        flush();

        List<String> kinds = listener.events.stream().map(Recorded::kind)
                .toList();
        assertTrue(
                kinds.indexOf("count-started") < kinds.indexOf("count-ended"),
                "count-started must precede count-ended");
        assertTrue(
                kinds.indexOf("fetch-started") < kinds.indexOf("fetch-ended"),
                "fetch-started must precede fetch-ended");
    }

    @Test
    void filteredQuery_isReportedAsFiltered() {
        DataCommunicator<String> communicator = flatCommunicator();
        ListDataProvider<String> provider = new ListDataProvider<>(items(20));
        communicator.setDataProvider(provider,
                (SerializablePredicateFilter) item -> item.endsWith("1"));
        communicator.setViewportRange(0, 20);
        flush();

        assertTrue(listener.first("count-ended").filtered(),
                "the count query carried a filter");
        assertTrue(listener.first("fetch-ended").filtered(),
                "the fetch query carried a filter");
    }

    @Test
    void failingFetch_reportsMinusOneRows() {
        DataCommunicator<String> communicator = flatCommunicator();
        communicator.setDataProvider(new AbstractBackEndDataProvider<>() {
            @Override
            protected Stream<String> fetchFromBackEnd(
                    Query<String, Object> query) {
                throw new IllegalStateException("backend is down");
            }

            @Override
            protected int sizeInBackEnd(Query<String, Object> query) {
                return 10;
            }
        }, null);
        communicator.setViewportRange(0, 10);

        assertThrows(IllegalStateException.class, this::flush);

        assertEquals(-1, listener.first("fetch-ended").result(),
                "a fetch that threw reports -1 rows");
        assertEquals(10, listener.first("count-ended").result(),
                "the count query still succeeded");

        assertEquals(1, listener.failures.size(),
                "the throwable is reported exactly once");
        assertEquals("backend is down", listener.failures.get(0).getMessage(),
                "fetchFailed carries the original throwable");

        List<String> kinds = listener.events.stream().map(Recorded::kind)
                .toList();
        assertTrue(kinds.indexOf("fetch-failed") < kinds.indexOf("fetch-ended"),
                "fetch-failed must precede fetch-ended");
    }

    @Test
    void failingCount_reportsFailedThenEnded() {
        DataCommunicator<String> communicator = flatCommunicator();
        communicator.setDataProvider(new AbstractBackEndDataProvider<>() {
            @Override
            protected Stream<String> fetchFromBackEnd(
                    Query<String, Object> query) {
                return Stream.empty();
            }

            @Override
            protected int sizeInBackEnd(Query<String, Object> query) {
                throw new IllegalStateException("count is down");
            }
        }, null);
        communicator.setViewportRange(0, 10);

        assertThrows(IllegalStateException.class, this::flush);

        assertEquals(1, listener.failures.size());
        assertEquals("count is down", listener.failures.get(0).getMessage(),
                "countFailed carries the original throwable");
        assertEquals(-1, listener.first("count-ended").result(),
                "a count that threw reports -1");

        List<String> kinds = listener.events.stream().map(Recorded::kind)
                .toList();
        assertTrue(kinds.indexOf("count-failed") < kinds.indexOf("count-ended"),
                "count-failed must precede count-ended");
    }

    @Test
    void removedListener_stopsReceivingEvents() {
        registration.remove();

        DataCommunicator<String> communicator = flatCommunicator();
        communicator.setDataProvider(itemProvider(10), null);
        communicator.setViewportRange(0, 10);
        flush();

        assertTrue(listener.events.isEmpty(),
                "removed listeners must not be notified");
    }

    @Test
    void throwingListener_doesNotBreakLoading() {
        service.getEventBus().addListener(DataCountStartedEvent.class,
                event -> {
                    throw new RuntimeException("listener blew up");
                });
        service.getEventBus().addListener(DataFetchEndedEvent.class, event -> {
            throw new RuntimeException("listener blew up");
        });

        DataCommunicator<String> communicator = flatCommunicator();
        communicator.setDataProvider(itemProvider(10), null);
        communicator.setViewportRange(0, 10);
        flush();

        assertEquals(10, listener.first("fetch-ended").result(),
                "loading completed despite a throwing listener");
    }

    // ---------- hierarchical ----------

    @Test
    void hierarchicalCommunicator_reportsRootLevelCountAndFetch() {
        hierarchicalCommunicator(treeData()).setViewportRange(0, 50);
        flush();

        Recorded count = listener.first("count-ended");
        assertEquals(2, count.result(), "two root items");
        assertSame(component, count.component());

        Recorded fetch = listener.first("fetch-ended");
        assertEquals(2, fetch.result(), "both root items were fetched");
        assertSame(component, fetch.component());
    }

    @Test
    void hierarchicalCommunicator_expandReportsAnExtraChildCount() {
        HierarchicalDataCommunicator<String> communicator = hierarchicalCommunicator(
                treeData());
        communicator.setViewportRange(0, 50);
        flush();

        int countsBeforeExpand = listener.of("count-ended").size();
        int fetchesBeforeExpand = listener.of("fetch-ended").size();

        communicator.expand(List.of("root-0"));
        flush();

        assertTrue(listener.of("count-ended").size() > countsBeforeExpand,
                "expanding an item must report a further count query");
        assertTrue(listener.of("fetch-ended").size() > fetchesBeforeExpand,
                "the children of an expanded item must be fetched");

        // root-0 has three children, so the child count is identifiable by
        // its result without the event naming the level.
        assertTrue(
                listener.of("count-ended").stream()
                        .anyMatch(e -> e.result() == 3),
                "the children of root-0 were counted");
    }

    // ---------- async ----------

    /**
     * A component using
     * {@link DataCommunicator#enablePushUpdates(java.util.concurrent.Executor)}
     * fetches on the executor thread, where none of the {@code getCurrent()}
     * thread locals are set. The reporting context therefore has to come from
     * the state node, and this test fails if it is ever derived from
     * {@link VaadinService#getCurrent()} instead.
     */
    @Test
    void asyncFetch_reportedWithoutCurrentInstance() throws Exception {
        ui.getPushConfiguration().setPushMode(PushMode.AUTOMATIC);

        AtomicReference<String> fetchThread = new AtomicReference<>();
        AtomicBoolean serviceCurrentWasSet = new AtomicBoolean(true);
        AtomicBoolean uiCurrentWasSet = new AtomicBoolean(true);
        AtomicReference<Component> resolved = new AtomicReference<>();
        CountDownLatch fired = new CountDownLatch(1);

        service.getEventBus().addListener(DataFetchStartedEvent.class,
                event -> {
                    fetchThread.set(Thread.currentThread().getName());
                    serviceCurrentWasSet
                            .set(VaadinService.getCurrent() != null);
                    uiCurrentWasSet.set(UI.getCurrent() != null);
                    assertNotNull(event.getUI(),
                            "the UI must be resolved from the state node");
                    resolved.set(event.getComponent().orElse(null));
                    fired.countDown();
                });

        DataCommunicator<String> communicator = flatCommunicator();
        communicator.setDataProvider(itemProvider(60), null);

        Executor executor = Executors.newSingleThreadExecutor(runnable -> {
            Thread thread = new Thread(runnable, "async-fetch");
            thread.setDaemon(true);
            return thread;
        });
        communicator.enablePushUpdates(executor);
        communicator.setViewportRange(0, 50);

        String requestThread = Thread.currentThread().getName();
        flush();

        assertTrue(fired.await(10, TimeUnit.SECONDS),
                "the fetch listener never fired on the async path");
        assertNotSame(requestThread, fetchThread.get(),
                "the fetch should have run off the request thread");
        assertFalse(serviceCurrentWasSet.get(),
                "VaadinService.getCurrent() is not available on the executor thread");
        assertFalse(uiCurrentWasSet.get(),
                "UI.getCurrent() is not available on the executor thread");
        assertSame(component, resolved.get(),
                "the component is still attributed on the async path");
    }

    // ---------- helpers ----------

    private interface SerializablePredicateFilter
            extends com.vaadin.flow.function.SerializablePredicate<String> {
    }

    private DataCommunicator<String> flatCommunicator() {
        return new DataCommunicator<>((item, json) -> {
        }, Mockito.mock(ArrayUpdater.class, Mockito.RETURNS_DEEP_STUBS),
                data -> {
                }, component.getElement().getNode());
    }

    private HierarchicalDataCommunicator<String> hierarchicalCommunicator(
            TreeData<String> data) {
        CompositeDataGenerator<String> generator = new CompositeDataGenerator<>();
        ArrayUpdater updater = Mockito.mock(ArrayUpdater.class);
        Mockito.when(updater.startUpdate(Mockito.anyInt()))
                .thenReturn(new ArrayUpdater.Update() {
                    @Override
                    public void clear(int start, int length) {
                    }

                    @Override
                    public void set(int start, List<JsonNode> items) {
                    }

                    @Override
                    public void commit(int updateId) {
                    }
                });
        HierarchicalDataCommunicator<String> communicator = new HierarchicalDataCommunicator<>(
                generator, updater, component.getElement().getNode(),
                () -> null);
        communicator.setDataProvider(new TreeDataProvider<>(data), null);
        return communicator;
    }

    private static TreeData<String> treeData() {
        TreeData<String> data = new TreeData<>();
        data.addItems(null, "root-0", "root-1");
        data.addItems("root-0", "child-0", "child-1", "child-2");
        return data;
    }

    private static ListDataProvider<String> itemProvider(int count) {
        return new ListDataProvider<>(items(count));
    }

    private static List<String> items(int count) {
        return IntStream.range(0, count).mapToObj(i -> "item-" + i).toList();
    }

    private void flush() {
        ui.getInternals().getStateTree().runExecutionsBeforeClientResponse();
    }
}
