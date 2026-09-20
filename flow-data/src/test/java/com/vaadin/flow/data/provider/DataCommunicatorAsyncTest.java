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

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import tools.jackson.databind.JsonNode;

import com.vaadin.flow.dom.Element;
import com.vaadin.flow.internal.Range;
import com.vaadin.flow.shared.communication.PushMode;
import com.vaadin.tests.util.MockUI;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DataCommunicatorAsyncTest {

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

    private boolean dataProviderWithParallelStream;

    @BeforeEach
    void init() {
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

    @ParameterizedTest
    @ValueSource(booleans = { true, false })
    void asyncExcutorPushDisabledThrows(
            boolean dataProviderWithParallelStream) {
        this.dataProviderWithParallelStream = dataProviderWithParallelStream;
        ui.getPushConfiguration().setPushMode(PushMode.DISABLED);
        dataCommunicator.setDataProvider(createDataProvider(), null);
        dataCommunicator.enablePushUpdates(executor);
        dataCommunicator.setViewportRange(0, 50);
        assertThrows(IllegalStateException.class,
                () -> fakeClientCommunication());
    }

    @ParameterizedTest
    @ValueSource(booleans = { true, false })
    void asyncRequestedRangeHappensLater(
            boolean dataProviderWithParallelStream) {
        this.dataProviderWithParallelStream = dataProviderWithParallelStream;
        latch = new CountDownLatch(1);
        ui.getPushConfiguration().setPushMode(PushMode.AUTOMATIC);
        dataCommunicator.setDataProvider(createDataProvider(), null);
        dataCommunicator.enablePushUpdates(executor);
        dataCommunicator.setViewportRange(0, 50);
        fakeClientCommunication();

        assertNotEquals(Range.withLength(0, 50), lastSet,
                "Expected initial reset not yet done.");

        try {
            latch.await();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        assertEquals(Range.withLength(0, 50), lastSet,
                "Expected initial full reset.");
        lastSet = null;

        element.removeFromParent();
        fakeClientCommunication();

        assertNull(lastSet, "Expected no during reattach.");

        ui.getElement().appendChild(element);
        fakeClientCommunication();

        latch = new CountDownLatch(1);

        try {
            latch.await();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        assertEquals(Range.withLength(0, 50), lastSet,
                "Expected initial full reset after reattach");
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
}
