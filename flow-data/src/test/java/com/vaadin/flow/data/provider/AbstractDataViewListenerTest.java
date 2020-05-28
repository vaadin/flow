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

import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public abstract class AbstractDataViewListenerTest {

    private final String[] ITEMS = new String[] { "one", "two", "three",
            "four" };

    @Test
    public void addSizeChangeListener_sizeChanged_listenersAreNotified() {
        HasListDataView<String, ? extends AbstractListDataView<String>> hasListDataView =
                getComponent();
        AbstractListDataView<String> dataView =
                hasListDataView.setDataProvider(Arrays.stream(ITEMS));

        AtomicInteger invocationCounter = new AtomicInteger(0);

        dataView.addSizeChangeListener(event -> invocationCounter.incrementAndGet());

        dataView.withFilter("one"::equals);
        dataView.withFilter(null);
        dataView.addItemAfter("five", "four");
        dataView.addItemBefore("zero", "one");
        dataView.addItem("last");
        dataView.removeItem("zero");

        Assert.assertEquals(
                "Unexpected count of size change listener invocations occurred",
                6, invocationCounter.get());
    }

    @Test
    public void addSizeChangeListener_sizeNotChanged_listenersAreNotNotified() {
        HasListDataView<String, ? extends AbstractListDataView<String>> hasListDataView =
                getComponent();
        AbstractListDataView<String> dataView =
                hasListDataView.setDataProvider(ITEMS);

        AtomicBoolean invocationChecker = new AtomicBoolean(false);

        dataView.addSizeChangeListener(event ->
                invocationChecker.getAndSet(true));

        dataView.withSortComparator(String::compareTo);

        Assert.assertFalse("Unexpected size change listener invocation",
                invocationChecker.get());
    }

    @Test
    public void addSizeChangeListener_sizeChanged_newSizeSuppliedInEvent() {
        HasListDataView<String, ? extends AbstractListDataView<String>> hasListDataView =
                getComponent();
        AbstractListDataView<String> dataView =
                hasListDataView.setDataProvider(ITEMS);

        dataView.addSizeChangeListener(event ->
                Assert.assertEquals("Unexpected data size",1, event.getSize()));

        dataView.withFilter("one"::equals);
    }

    protected abstract HasListDataView<String, ? extends AbstractListDataView<String>> getComponent();
}
