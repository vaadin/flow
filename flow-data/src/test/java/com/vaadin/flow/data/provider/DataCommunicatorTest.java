/*
 * Copyright 2000-2017 Vaadin Ltd.
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
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.dom.Element;
import com.vaadin.flow.internal.Range;
import com.vaadin.flow.server.VaadinRequest;
import com.vaadin.flow.server.VaadinService;
import com.vaadin.flow.server.VaadinSession;

import elemental.json.JsonValue;

public class DataCommunicatorTest {

    private DataCommunicator<String> dataCommunicator;

    @Mock
    private DataGenerator<String> dataGenerator;
    @Mock
    private ArrayUpdater arrayUpdater;

    private Element element;
    private MockUI ui;

    private ArrayUpdater.Update update;

    public Range lastClear = null;
    public Range lastSet = null;
    public int lastUpdateId = -1;

    @Before
    public void init() {
        MockitoAnnotations.initMocks(this);
        ui = new MockUI();
        element = new Element("div");
        ui.getElement().appendChild(element);

        lastClear = null;
        lastSet = null;
        lastUpdateId = -1;

        update = new ArrayUpdater.Update() {

            @Override
            public void clear(int start, int length) {
                lastClear = Range.withLength(start, length);
            }

            @Override
            public void set(int start, List<JsonValue> items) {
                lastSet = Range.withLength(start, items.size());
            }

            @Override
            public void commit(int updateId) {
                lastUpdateId = updateId;
            }
        };

        Mockito.when(arrayUpdater.startUpdate(Mockito.anyInt()))
                .thenReturn(update);

        dataCommunicator = new DataCommunicator<>(dataGenerator, arrayUpdater,
                data -> {
                }, element.getNode());
    }

    @Test
    public void communicator_with_0_items_should_not_refresh_all() {
        dataCommunicator.setRequestedRange(0, 0);
        // Fake client communication.
        ui.getInternals().getStateTree().runExecutionsBeforeClientResponse();

        Assert.assertEquals(Range.withLength(0, 0), lastSet);
        Assert.assertNull(
                "Only requestAll should clear items. This may make us loop.",
                lastClear);

        dataCommunicator.setRequestedRange(0, 0);
        // Fake client communication.
        ui.getInternals().getStateTree().runExecutionsBeforeClientResponse();

        Assert.assertEquals(Range.withLength(0, 0), lastSet);
        Assert.assertNull(
                "Only requestAll should clear items. Which would make us loop.",
                lastClear);
    }

    @Test
    public void communicator_with_items_should_send_updates_but_not_refresh_all() {
        dataCommunicator
                .setDataProvider(new AbstractDataProvider<String, Object>() {
                    @Override
                    public boolean isInMemory() {
                        return true;
                    }

                    @Override
                    public int size(Query<String, Object> query) {
                        return 100;
                    }

                    @Override
                    public Stream<String> fetch(Query<String, Object> query) {
                        return IntStream
                                .range(query.getOffset(),
                                        query.getLimit() + query.getOffset())
                                .mapToObj(Integer::toString);
                    }
                }, null);

        dataCommunicator.setRequestedRange(0, 50);
        // Fake client communication.
        ui.getInternals().getStateTree().runExecutionsBeforeClientResponse();

        Assert.assertEquals(
                "Expected request range for 50 items on first request.",
                Range.withLength(0, 50), lastSet);

        dataCommunicator.setRequestedRange(0, 70);
        // Fake client communication.
        ui.getInternals().getStateTree().runExecutionsBeforeClientResponse();
        Assert.assertEquals("Expected request range for 20 new items.",
                Range.withLength(50, 20), lastSet);
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
                session = new AlwaysLockedVaadinSession(null);
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
