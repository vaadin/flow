/**
 * Copyright (C) 2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See {@literal <https://vaadin.com/commercial-license-and-service-terms>}  for the full
 * license.
 */
package com.vaadin.flow.data.provider;

import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;

import org.junit.Assert;
import org.junit.Test;

import com.vaadin.flow.data.provider.AbstractDataProvider;
import com.vaadin.flow.data.provider.DataChangeEvent;
import com.vaadin.flow.data.provider.Query;
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
}
