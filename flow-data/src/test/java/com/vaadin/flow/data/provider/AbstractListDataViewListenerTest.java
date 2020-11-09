/*
 * Copyright 2000-2020 Vaadin Ltd.
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

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.function.SerializablePredicate;
import com.vaadin.flow.server.VaadinRequest;
import com.vaadin.flow.server.VaadinService;
import com.vaadin.flow.server.VaadinSession;
import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public abstract class AbstractListDataViewListenerTest {

    @Test
    public void addItemCountChangeListener_itemsCountChanged_listenersAreNotified() {
        String[] items = new String[] { "item1", "item2", "item3", "item4" };
        HasListDataView<String, ? extends AbstractListDataView<String>> component =
                getVerifiedComponent();
        AbstractListDataView<String> dataView = component
                .setItems(new ArrayList<>(Arrays.asList(items)));

        AtomicInteger invocationCounter = new AtomicInteger(0);

        dataView.addItemCountChangeListener(
                event -> invocationCounter.incrementAndGet());

        UI ui = new MockUI();
        ui.add((Component) component);

        dataView.setFilter("one"::equals);
        dataView.setFilter(null);
        dataView.addItemAfter("item5", "item4");
        dataView.addItemBefore("item0", "item1");
        dataView.addItem("last");
        dataView.removeItem("item0");

        fakeClientCall(ui);

        Assert.assertEquals(
                "Unexpected number of item count change listener invocations " +
                        "occurred", 1, invocationCounter.get());
    }

    @Test
    public void addItemCountChangeListener_itemsCountNotChanged_listenersAreNotNotified() {
        String[] items = new String[] { "item1", "item2", "item3", "item4" };
        HasListDataView<String, ? extends AbstractListDataView<String>> component =
                getVerifiedComponent();
        AbstractListDataView<String> dataView = component
                .setItems(items);

        AtomicBoolean invocationChecker = new AtomicBoolean(false);

        UI ui = new MockUI();
        ui.add((Component) component);

        // Make initial item count change
        fakeClientCall(ui);

        dataView.addItemCountChangeListener(
                event -> invocationChecker.getAndSet(true));

        dataView.setSortComparator(String::compareTo);

        // Make item count change after sort. No event should be sent as item
        // count stays the same.
        fakeClientCall(ui);

        Assert.assertFalse("Unexpected item count listener invocation",
                invocationChecker.get());
    }

    @Test
    public void addItemCountChangeListener_itemsCountChanged_newItemCountSuppliedInEvent() {
        String[] items = new String[] { "item1", "item2", "item3", "item4" };
        HasListDataView<String, ? extends AbstractListDataView<String>> component =
                getVerifiedComponent();
        AbstractListDataView<String> dataView = component
                .setItems(items);

        AtomicBoolean invocationChecker = new AtomicBoolean(false);

        UI ui = new MockUI();
        ui.add((Component) component);

        // Make initial item count event
        fakeClientCall(ui);

        dataView.addItemCountChangeListener(event -> {
            Assert.assertEquals("Unexpected item count", 1,
                    event.getItemCount());
            Assert.assertFalse(event.isItemCountEstimated());
            invocationChecker.set(true);
        });

        dataView.setFilter("item1"::equals);

        // Item count change should be sent as item count has changed after
        // filtering.
        fakeClientCall(ui);

        Assert.assertTrue("Item count change never called",
                invocationChecker.get());
    }

    @Test
    public void setItems_setNewItemsToComponent_filteringAndSortingRemoved() {
        HasListDataView<String, ? extends AbstractListDataView<String>> component = getVerifiedComponent();

        AbstractListDataView<String> listDataView = component.setItems("item1",
                "item2", "item3");

        SerializablePredicate<String> filter = "item2"::equals;

        listDataView.setFilter(filter);

        Assert.assertEquals("Unexpected filtered item count", 1,
                listDataView.getItemCount());

        listDataView = component.setItems("item1", "item2", "item3");

        Assert.assertEquals("Non-filtered item count expected", 3,
                listDataView.getItemCount());
    }

    protected abstract HasListDataView<String, ? extends AbstractListDataView<String>> getComponent();

    private void fakeClientCall(UI ui) {
        ui.getInternals().getStateTree().runExecutionsBeforeClientResponse();
        ui.getInternals().getStateTree().collectChanges(ignore -> {
        });
    }

    private static class MockUI extends UI {

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

    private static class AlwaysLockedVaadinSession extends MockVaadinSession {

        public AlwaysLockedVaadinSession(VaadinService service) {
            super(service);
            lock();
        }

    }

    private static class MockVaadinSession extends VaadinSession {
        /*
         * Used to make sure there's at least one reference to the mock session
         * while it's locked. This is used to prevent the session from being
         * eaten by GC in tests where @Before creates a session and sets it as
         * the current instance without keeping any direct reference to it. This
         * pattern has a chance of leaking memory if the session is not unlocked
         * in the right way, but it should be acceptable for testing use.
         */
        private static final ThreadLocal<MockVaadinSession> referenceKeeper = new ThreadLocal<>();
        private ReentrantLock lock = new ReentrantLock();

        public MockVaadinSession(VaadinService service) {
            super(service);
        }

        @Override
        public void close() {
            super.close();
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
    }

    private HasListDataView<String, ? extends AbstractListDataView<String>> getVerifiedComponent() {
        HasListDataView<String, ? extends AbstractListDataView<String>> component = getComponent();
        if (component instanceof Component) {
            return component;
        }
        throw new IllegalArgumentException(String.format(
                "Component subclass is expected, but was given a '%s'",
                component.getClass().getSimpleName()));
    }
}
