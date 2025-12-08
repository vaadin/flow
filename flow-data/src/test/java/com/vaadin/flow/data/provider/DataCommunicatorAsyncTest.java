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
package com.vaadin.flow.data.provider;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import tools.jackson.databind.JsonNode;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.dom.Element;
import com.vaadin.flow.internal.Range;
import com.vaadin.flow.server.RouteRegistry;
import com.vaadin.flow.server.VaadinRequest;
import com.vaadin.flow.server.VaadinService;
import com.vaadin.flow.server.VaadinServletService;
import com.vaadin.flow.server.VaadinSession;
import com.vaadin.flow.shared.communication.PushMode;

@RunWith(Parameterized.class)
public class DataCommunicatorAsyncTest {

    /**
     * Test item that uses id for identity.
     */
    private static class Item {
        private final int id;
        private String value;

        public Item(int id) {
            this(id, "Item " + id);
        }

        public Item(int id, String value) {
            this.id = id;
            this.value = value;
        }

        @Override
        public String toString() {
            return id + ": " + value;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj instanceof Item) {
                Item that = (Item) obj;
                return that.id == id;
            } else {
                return false;
            }
        }

        @Override
        public int hashCode() {
            return id;
        }
    }

    private DataCommunicator<Item> dataCommunicator;

    @Mock
    private DataGenerator<Item> dataGenerator;
    @Mock
    private ArrayUpdater arrayUpdater;

    private Element element;
    private MockUI ui;

    private ArrayUpdater.Update update;

    private CountDownLatch latch;
    private Executor executor;

    public Range lastClear = null;
    public Range lastSet = null;
    public int lastUpdateId = -1;

    private final boolean dataProviderWithParallelStream;

    public DataCommunicatorAsyncTest(boolean dataProviderWithParallelStream) {
        this.dataProviderWithParallelStream = dataProviderWithParallelStream;
    }

    @Parameterized.Parameters
    public static Collection<Boolean> testParameters() {
        // Runs tests with both sequential and parallel data provider streams
        return Arrays.asList(false, true);
    }

    @Before
    public void init() {
        MockitoAnnotations.initMocks(this);
        ui = new MockUI();
        element = new Element("div");
        ui.getElement().appendChild(element);
        lastClear = null;
        lastSet = null;
        lastUpdateId = -1;
        executor = Executors.newCachedThreadPool();

        update = new ArrayUpdater.Update() {

            @Override
            public void clear(int start, int length) {
                lastClear = Range.withLength(start, length);
            }

            @Override
            public void set(int start, List<JsonNode> items) {
                lastSet = Range.withLength(start, items.size());
            }

            @Override
            public void commit(int updateId) {
                lastUpdateId = updateId;
            }
        };

        dataCommunicator = new DataCommunicator<>(dataGenerator, arrayUpdater,
                data -> {
                }, element.getNode());
    }

    @Test(expected = IllegalStateException.class)
    public void asyncExcutorPushDisabledThrows() {
        ui.getPushConfiguration().setPushMode(PushMode.DISABLED);
        dataCommunicator.setDataProvider(createDataProvider(), null);
        dataCommunicator.enablePushUpdates(executor);
        dataCommunicator.setViewportRange(0, 50);
        fakeClientCommunication();
    }

    @Test
    public void asyncRequestedRangeHappensLater() {
        latch = new CountDownLatch(1);
        ui.getPushConfiguration().setPushMode(PushMode.AUTOMATIC);
        dataCommunicator.setDataProvider(createDataProvider(), null);
        dataCommunicator.enablePushUpdates(executor);
        dataCommunicator.setViewportRange(0, 50);
        fakeClientCommunication();

        Assert.assertNotEquals("Expected initial reset not yet done.",
                Range.withLength(0, 50), lastSet);

        try {
            latch.await();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        Assert.assertEquals("Expected initial full reset.",
                Range.withLength(0, 50), lastSet);
        lastSet = null;

        element.removeFromParent();
        fakeClientCommunication();

        Assert.assertNull("Expected no during reattach.", lastSet);

        ui.getElement().appendChild(element);
        fakeClientCommunication();

        latch = new CountDownLatch(1);

        try {
            latch.await();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        Assert.assertEquals("Expected initial full reset after reattach",
                Range.withLength(0, 50), lastSet);
    }

    private AbstractDataProvider<Item, Object> createDataProvider() {
        return createDataProvider(100);
    }

    private AbstractDataProvider<Item, Object> createDataProvider(int items) {
        return new AbstractDataProvider<Item, Object>() {
            @Override
            public boolean isInMemory() {
                return true;
            }

            @Override
            public int size(Query<Item, Object> query) {
                return items;
            }

            @Override
            public Stream<Item> fetch(Query<Item, Object> query) {
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
                // Note: Mockito is not able to mock background call, thus
                // setting lastSet here
                lastSet = Range.withLength(query.getOffset(), query.getLimit());
                latch.countDown();
                return asParallelIfRequired(IntStream.range(query.getOffset(),
                        query.getLimit() + query.getOffset()))
                        .mapToObj(Item::new);
            }
        };
    }

    private IntStream asParallelIfRequired(IntStream stream) {
        if (dataProviderWithParallelStream) {
            return stream.parallel();
        }
        return stream;
    }

    private void fakeClientCommunication() {
        ui.getInternals().getStateTree().runExecutionsBeforeClientResponse();
        ui.getInternals().getStateTree().collectChanges(ignore -> {
        });
    }

    public static class MockUI extends UI {

        public MockUI() {
            this(findOrcreateSession());
        }

        public MockUI(VaadinSession session) {
            getInternals().setSession(session);
            setCurrent(this);
        }

        @Override
        protected void init(VaadinRequest request) {
            // Do nothing
        }

        private static VaadinSession findOrcreateSession() {
            VaadinSession session = VaadinSession.getCurrent();
            if (session == null) {
                RouteRegistry routeRegistry = Mockito.mock(RouteRegistry.class);
                VaadinServletService service = new VaadinServletService() {
                    @Override
                    protected RouteRegistry getRouteRegistry() {
                        return routeRegistry;
                    }
                };
                session = new AlwaysLockedVaadinSession(service);
                VaadinSession.setCurrent(session);
            }
            return session;
        }
    }

    public static class AlwaysLockedVaadinSession extends MockVaadinSession {

        public AlwaysLockedVaadinSession(VaadinService service) {
            super(service);
            lock();
        }

    }

    public static class MockVaadinSession extends VaadinSession {
        /*
         * Used to make sure there's at least one reference to the mock session
         * while it's locked. This is used to prevent the session from being
         * eaten by GC in tests where @Before creates a session and sets it as
         * the current instance without keeping any direct reference to it. This
         * pattern has a chance of leaking memory if the session is not unlocked
         * in the right way, but it should be acceptable for testing use.
         */
        private static final ThreadLocal<MockVaadinSession> referenceKeeper = new ThreadLocal<>();

        public MockVaadinSession(VaadinService service) {
            super(service);
        }

        @Override
        public void close() {
            super.close();
            closeCount++;
        }

        public int getCloseCount() {
            return closeCount;
        }

        @Override
        public Lock getLockInstance() {
            return lock;
        }

        @Override
        public void lock() {
            super.lock();
            referenceKeeper.set(this);
        }

        @Override
        public void unlock() {
            super.unlock();
            referenceKeeper.remove();
        }

        private int closeCount;

        private ReentrantLock lock = new ReentrantLock();
    }
}
