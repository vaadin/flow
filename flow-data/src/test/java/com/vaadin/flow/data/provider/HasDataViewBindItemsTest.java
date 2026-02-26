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

import org.junit.jupiter.api.Test;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.dom.SignalsUnitTest;
import com.vaadin.flow.function.SerializableSupplier;
import com.vaadin.flow.signals.local.ListSignal;
import com.vaadin.flow.signals.local.ValueSignal;
import com.vaadin.flow.signals.shared.SharedListSignal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class HasDataViewBindItemsTest extends SignalsUnitTest {

    @Tag("test-component")
    private static class TestComponent extends Component
            implements HasDataView<String, Void, TestDataView> {

        private InMemoryDataProvider<String> dataProvider;
        private TestDataView dataView;

        @Override
        @SuppressWarnings({ "unchecked", "rawtypes" })
        public TestDataView setItems(DataProvider<String, Void> dataProvider) {
            if (dataProvider instanceof InMemoryDataProvider) {
                this.dataProvider = (InMemoryDataProvider) dataProvider;
            }
            this.dataView = new TestDataView(() -> dataProvider, this);
            return this.dataView;
        }

        @Override
        public TestDataView setItems(
                InMemoryDataProvider<String> dataProvider) {
            this.dataProvider = dataProvider;
            this.dataView = new TestDataView(() -> dataProvider, this);
            return this.dataView;
        }

        @Override
        public TestDataView getGenericDataView() {
            if (dataView == null) {
                throw new IllegalStateException(
                        "No data provider has been set");
            }
            return dataView;
        }

        public InMemoryDataProvider<String> getDataProvider() {
            return dataProvider;
        }
    }

    private static class TestDataView extends AbstractDataView<String> {
        private int refreshAllCount = 0;
        private final List<String> refreshedItems = new ArrayList<>();

        public TestDataView(
                SerializableSupplier<? extends DataProvider<String, ?>> dataProviderSupplier,
                Component component) {
            super(dataProviderSupplier, component);
        }

        @Override
        protected Class<?> getSupportedDataProviderType() {
            return InMemoryDataProvider.class;
        }

        @Override
        public void refreshAll() {
            refreshAllCount++;
            super.refreshAll();
        }

        @Override
        public void refreshItem(String item) {
            refreshedItems.add(item);
            super.refreshItem(item);
        }

        public int getItemCount() {
            return (int) getItems().count();
        }

        public String getItem(int index) {
            return getItems().skip(index).findFirst().orElse(null);
        }

        public int getRefreshAllCount() {
            return refreshAllCount;
        }

        public List<String> getRefreshedItems() {
            return refreshedItems;
        }

        public void resetCounters() {
            refreshAllCount = 0;
            refreshedItems.clear();
        }
    }

    @Test
    public void bindItems_listSignal_insertsItems() {
        TestComponent component = new TestComponent();
        UI.getCurrent().add(component);

        ListSignal<String> itemsSignal = new ListSignal<>();
        itemsSignal.insertLast("Item 1");
        itemsSignal.insertLast("Item 2");
        itemsSignal.insertLast("Item 3");

        component.bindItems(itemsSignal);

        TestDataView dataView = component.getGenericDataView();
        assertEquals(3, dataView.getItemCount());
        assertEquals("Item 1", dataView.getItem(0));
        assertEquals("Item 2", dataView.getItem(1));
        assertEquals("Item 3", dataView.getItem(2));
    }

    @Test
    public void bindItems_outerSignalChange_triggersRefreshAll() {
        TestComponent component = new TestComponent();
        UI.getCurrent().add(component);

        ListSignal<String> itemsSignal = new ListSignal<>();
        itemsSignal.insertLast("Item 1");
        itemsSignal.insertLast("Item 2");

        component.bindItems(itemsSignal);
        TestDataView dataView = component.getGenericDataView();

        // Initial setup should have called refreshAll once
        assertEquals(1, dataView.getRefreshAllCount());

        // Add a new item (outer signal change)
        itemsSignal.insertLast("Item 3");

        // Verify refreshAll was called again
        assertEquals(2, dataView.getRefreshAllCount());
        assertEquals(3, dataView.getItemCount());
        assertEquals("Item 3", dataView.getItem(2));
    }

    @Test
    public void bindItems_innerSignalChange_triggersRefreshItem() {
        TestComponent component = new TestComponent();
        UI.getCurrent().add(component);

        ListSignal<String> itemsSignal = new ListSignal<>();
        itemsSignal.insertLast("Item 1");
        itemsSignal.insertLast("Item 2");

        component.bindItems(itemsSignal);
        TestDataView dataView = component.getGenericDataView();

        // Get the inner signals using peek() to avoid reactive tracking
        ValueSignal<String> item1Signal = itemsSignal.peek().getFirst();

        // Reset counters after initial setup
        dataView.resetCounters();

        // Change an inner signal value
        item1Signal.set("Updated Item 1");

        // Verify refreshItem was called
        assertEquals(0, dataView.getRefreshAllCount());
        assertEquals(1, dataView.getRefreshedItems().size());
        assertEquals("Updated Item 1", dataView.getRefreshedItems().getFirst());

        // Verify the data is updated
        assertEquals("Updated Item 1", dataView.getItem(0));
    }

    @Test
    public void bindItems_multipleInnerSignalChanges_triggersMultipleRefreshItems() {
        TestComponent component = new TestComponent();
        UI.getCurrent().add(component);

        ListSignal<String> itemsSignal = new ListSignal<>();
        itemsSignal.insertLast("Item 1");
        itemsSignal.insertLast("Item 2");
        itemsSignal.insertLast("Item 3");

        component.bindItems(itemsSignal);
        TestDataView dataView = component.getGenericDataView();

        // Get the inner signals using peek() to avoid reactive tracking
        ValueSignal<String> item1Signal = itemsSignal.peek().get(0);
        ValueSignal<String> item3Signal = itemsSignal.peek().get(2);

        // Reset counters after initial setup
        dataView.resetCounters();

        // Change multiple inner signals
        item1Signal.set("Updated Item 1");
        item3Signal.set("Updated Item 3");

        // Verify refreshItem was called twice
        assertEquals(0, dataView.getRefreshAllCount());
        assertEquals(2, dataView.getRefreshedItems().size());
        assertTrue(dataView.getRefreshedItems().contains("Updated Item 1"));
        assertTrue(dataView.getRefreshedItems().contains("Updated Item 3"));
    }

    @Test
    public void bindItems_mixedChanges_correctRefreshCalls() {
        TestComponent component = new TestComponent();
        UI.getCurrent().add(component);

        ListSignal<String> itemsSignal = new ListSignal<>();
        itemsSignal.insertLast("Item 1");

        component.bindItems(itemsSignal);
        TestDataView dataView = component.getGenericDataView();

        // Get the inner signal using peek() to avoid reactive tracking
        ValueSignal<String> item1Signal = itemsSignal.peek().getFirst();

        // Reset counters after initial setup
        dataView.resetCounters();

        // Inner signal change
        item1Signal.set("Updated Item 1");
        assertEquals(0, dataView.getRefreshAllCount());
        assertEquals(1, dataView.getRefreshedItems().size());

        // Outer signal change
        itemsSignal.insertLast("Item 2");
        assertEquals(1, dataView.getRefreshAllCount());
        // After refreshAll, inner effects should not call refreshItem on
        // initial
        // setup
        assertEquals(1, dataView.getRefreshedItems().size());

        // Get the updated reference to item1Signal after list structure change
        // using peek()
        item1Signal = itemsSignal.peek().getFirst();

        // Another inner signal change
        item1Signal.set("Updated Again");
        assertEquals(1, dataView.getRefreshAllCount());
        assertEquals(2, dataView.getRefreshedItems().size());
    }

    @Test
    public void bindItems_emptyList_handledCorrectly() {
        TestComponent component = new TestComponent();
        UI.getCurrent().add(component);

        ListSignal<String> itemsSignal = new ListSignal<>();

        component.bindItems(itemsSignal);
        TestDataView dataView = component.getGenericDataView();

        assertEquals(0, dataView.getItemCount());

        // Add item to an empty list
        itemsSignal.insertLast("First Item");
        assertEquals(1, dataView.getItemCount());
    }

    @Test
    public void bindItems_removeItems_triggersRefreshAll() {
        TestComponent component = new TestComponent();
        UI.getCurrent().add(component);

        ListSignal<String> itemsSignal = new ListSignal<>();
        itemsSignal.insertLast("Item 1");
        itemsSignal.insertLast("Item 2");

        component.bindItems(itemsSignal);
        TestDataView dataView = component.getGenericDataView();

        // Get reference to the first item signal to remove it using peek()
        ValueSignal<String> item1Signal = itemsSignal.peek().getFirst();

        dataView.resetCounters();

        // Remove an item
        itemsSignal.remove(item1Signal);

        assertEquals(1, dataView.getRefreshAllCount());
        assertEquals(1, dataView.getItemCount());
        assertEquals("Item 2", dataView.getItem(0));
    }

    @Test
    public void bindItems_nullSignal_throwsException() {
        TestComponent component = new TestComponent();
        UI.getCurrent().add(component);

        assertThrows(NullPointerException.class,
                () -> component.bindItems(null));
    }

    @Test
    public void bindItems_alreadyBound_throwsBindingActiveException() {
        TestComponent component = new TestComponent();
        UI.getCurrent().add(component);

        ListSignal<String> itemsSignal = new ListSignal<>();
        itemsSignal.insertLast("Item 1");

        // First binding should succeed
        component.bindItems(itemsSignal);

        // Second binding should throw BindingActiveException
        ListSignal<String> secondSignal = new ListSignal<>();
        secondSignal.insertLast("Item 2");

        assertThrows(com.vaadin.flow.signals.BindingActiveException.class,
                () -> component.bindItems(secondSignal));
    }

    @Test
    public void bindItems_componentDetached_effectsStopWorking() {
        TestComponent component = new TestComponent();
        UI ui = UI.getCurrent();
        ui.add(component);

        ListSignal<String> itemsSignal = new ListSignal<>();
        itemsSignal.insertLast("Item 1");

        component.bindItems(itemsSignal);
        TestDataView dataView = component.getGenericDataView();

        // Get the inner signal using peek() to avoid reactive tracking
        ValueSignal<String> item1Signal = itemsSignal.peek().getFirst();

        // Detach component
        ui.remove(component);

        dataView.resetCounters();

        // Change signal while detached - should not trigger updates
        item1Signal.set("Updated while detached");

        // No refresh should have been called
        assertEquals(0, dataView.getRefreshAllCount());
        assertEquals(0, dataView.getRefreshedItems().size());
    }

    @Test
    public void bindItems_componentReattached_effectsResumeWorking() {
        TestComponent component = new TestComponent();
        UI ui = UI.getCurrent();
        ui.add(component);

        ListSignal<String> itemsSignal = new ListSignal<>();
        itemsSignal.insertLast("Item 1");

        component.bindItems(itemsSignal);
        TestDataView dataView = component.getGenericDataView();

        // Get the inner signal using peek() to avoid reactive tracking
        ValueSignal<String> item1Signal = itemsSignal.peek().getFirst();

        // Detach and reattach
        ui.remove(component);
        ui.add(component);

        dataView.resetCounters();

        // Change signal after reattaching - should trigger updates
        item1Signal.set("Updated after reattach");

        assertFalse(dataView.getRefreshedItems().isEmpty());
    }

    @Test
    public void bindItems_sharedListSignal_itemsInsertedAndUpdated() {
        TestComponent component = new TestComponent();
        UI.getCurrent().add(component);

        SharedListSignal<String> itemsSignal = new SharedListSignal<>(
                String.class);
        itemsSignal.insertLast("Item 1");
        itemsSignal.insertLast("Item 2");

        component.bindItems(itemsSignal);

        TestDataView dataView = component.getGenericDataView();
        assertEquals(2, dataView.getItemCount());
        assertEquals("Item 1", dataView.getItem(0));
        assertEquals("Item 2", dataView.getItem(1));

        // Reset counters after initial setup
        dataView.resetCounters();

        // Change an inner signal value
        var item1Signal = itemsSignal.peek().getFirst();
        item1Signal.set("Updated Item 1");

        // Verify refreshItem was called
        assertEquals(0, dataView.getRefreshAllCount());
        assertEquals(1, dataView.getRefreshedItems().size());
        assertEquals("Updated Item 1", dataView.getRefreshedItems().getFirst());

        // Test structural change
        dataView.resetCounters();
        itemsSignal.insertLast("Item 3");

        // Verify refreshAll was called for structural change
        assertEquals(1, dataView.getRefreshAllCount());
        assertEquals(3, dataView.getItemCount());
    }

    @Test
    public void bindItems_genericSignal_worksCorrectly() {
        TestComponent component = new TestComponent();
        UI.getCurrent().add(component);

        // Create a generic Signal<List<ValueSignal<T>>> manually
        List<ValueSignal<String>> signalList = new ArrayList<>();
        signalList.add(new ValueSignal<>("Item 1"));
        signalList.add(new ValueSignal<>("Item 2"));

        // Create a simple signal wrapper that holds the list
        var itemsSignal = new ValueSignal<>(signalList);

        component.bindItems(itemsSignal);

        TestDataView dataView = component.getGenericDataView();
        assertEquals(2, dataView.getItemCount());
        assertEquals("Item 1", dataView.getItem(0));
        assertEquals("Item 2", dataView.getItem(1));

        // Reset counters after initial setup
        dataView.resetCounters();

        // Change an inner signal value
        signalList.getFirst().set("Updated Item 1");

        // Verify refreshItem was called
        assertEquals(0, dataView.getRefreshAllCount());
        assertEquals(1, dataView.getRefreshedItems().size());
        assertEquals("Updated Item 1", dataView.getRefreshedItems().getFirst());

        // Verify the data is updated
        assertEquals("Updated Item 1", dataView.getItem(0));
    }
}
