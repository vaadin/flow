/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.data.provider;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;

import org.junit.Assert;
import org.junit.Test;

import com.vaadin.flow.shared.Registration;

/**
 * @author Vaadin Ltd
 * @since 1.0
 *
 */
public class AbstractDataProviderTest {

    private static class TestDataProvider
            extends AbstractDataProvider<Object, Object> {

        @Override
        public Stream<Object> fetch(Query<Object, Object> t) {
            return null;
        }

        @Override
        public int size(Query<Object, Object> t) {
            return 0;
        }

        @Override
        public boolean isInMemory() {
            return false;
        }
    }

    @Test
    public void refreshAll_notifyListeners() {
        TestDataProvider dataProvider = new TestDataProvider();
        AtomicReference<DataChangeEvent<Object>> event = new AtomicReference<>();
        dataProvider.addDataProviderListener(ev -> {
            Assert.assertNull(event.get());
            event.set(ev);
        });
        dataProvider.refreshAll();
        Assert.assertNotNull(event.get());
        Assert.assertEquals(dataProvider, event.get().getSource());
    }

    @Test
    public void removeListener_listenerIsNotNotified() {
        TestDataProvider dataProvider = new TestDataProvider();
        AtomicReference<DataChangeEvent<Object>> event = new AtomicReference<>();
        Registration registration = dataProvider
                .addDataProviderListener(event::set);
        registration.remove();
        dataProvider.refreshAll();
        Assert.assertNull(event.get());
    }

    @Test
    public void eventUnregisterListener_insideListener() {
        TestDataProvider provider = new TestDataProvider();
        AtomicBoolean eventIsFired = new AtomicBoolean();
        provider.addListener(DataChangeEvent.class, event -> {
            eventIsFired.set(true);
            event.unregisterListener();
        });

        AtomicBoolean anotherListener = new AtomicBoolean();
        provider.addListener(DataChangeEvent.class, event -> {
            anotherListener.set(true);
        });

        provider.fireEvent(new DataChangeEvent<>(provider));
        Assert.assertTrue(eventIsFired.get());
        Assert.assertTrue(anotherListener.get());

        eventIsFired.set(false);
        anotherListener.set(false);
        provider.fireEvent(new DataChangeEvent<>(provider));
        Assert.assertFalse(eventIsFired.get());
        Assert.assertTrue(anotherListener.get());
    }

    @Test(expected = IllegalStateException.class)
    public void eventUnregisterListener_outsideListener() {
        TestDataProvider provider = new TestDataProvider();
        AtomicReference<DataChangeEvent> savedEvent = new AtomicReference();
        provider.addListener(DataChangeEvent.class, event -> {
            savedEvent.set(event);
        });

        provider.fireEvent(new DataChangeEvent<>(provider));
        savedEvent.get().unregisterListener();
    }

    @Test
    public void eventUnregisterListener_insideListener_listenerThrows_listenerIsUnregistered() {
        TestDataProvider provider = new TestDataProvider();
        AtomicBoolean eventIsFired = new AtomicBoolean();
        provider.addListener(DataChangeEvent.class, event -> {
            eventIsFired.set(true);
            event.unregisterListener();
            throw new NullPointerException();
        });

        try {
            provider.fireEvent(new DataChangeEvent<>(provider));
            Assert.fail();
        } catch (NullPointerException ignore) {
            eventIsFired.set(false);
            provider.fireEvent(new DataChangeEvent<>(provider));
            Assert.assertFalse(eventIsFired.get());
        }
    }

}
