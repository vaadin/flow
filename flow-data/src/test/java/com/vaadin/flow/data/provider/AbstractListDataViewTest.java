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
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.function.SerializableBiConsumer;
import com.vaadin.flow.function.SerializableComparator;
import com.vaadin.flow.function.SerializablePredicate;
import com.vaadin.flow.function.SerializableSupplier;
import com.vaadin.flow.function.ValueProvider;
import com.vaadin.flow.shared.Registration;
import com.vaadin.flow.tests.data.bean.Item;

import static com.vaadin.flow.tests.server.ClassesSerializableUtils.serializeAndDeserialize;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AbstractListDataViewTest {

    private Collection<String> items;

    private ListDataProvider<String> dataProvider;

    private AbstractListDataView<String> dataView;

    private Component component;

    private AbstractListDataView<Item> beanDataView;
    private ListDataProvider<Item> itemListDataProvider;

    @BeforeEach
    void init() {
        items = new ArrayList<>(Arrays.asList("first", "middle", "last"));
        dataProvider = DataProvider.ofCollection(items);
        component = new TestComponent();
        dataView = new ListDataViewImpl(() -> dataProvider, component);

        itemListDataProvider = DataProvider.ofCollection(getTestItems());
        beanDataView = new ItemListDataView(() -> itemListDataProvider,
                component);
    }

    @Test
    void createListDataViewInstance_faultyDataProvider_throwsException() {
        DataProvider dataProvider = DataProvider
                .fromCallbacks(query -> Stream.of("one"), query -> 1);
        var ex = assertThrows(IllegalStateException.class,
                () -> new ListDataViewImpl(() -> dataProvider, null));
        assertTrue(ex.getMessage()
                .contains("ListDataViewImpl only supports 'ListDataProvider' "
                        + "or it's subclasses, but was given a "
                        + "'AbstractBackEndDataProvider'"));
    }

    @Test
    void getItemCount_noFiltersSet_dataSizeObtained() {
        assertEquals(items.size(), dataView.getItemCount(),
                "Unexpected item count");
    }

    @Test
    void getItemCount_filtersSet_filteredItemsObtained() {
        dataProvider.setFilter(item -> item.equals("first"));
        assertEquals(1, dataView.getItemCount(), "Unexpected item count");
    }

    @Test
    void getNextItem_nextItemAvailable_nextItemFound() {
        Optional<String> middle = dataView.getNextItem("middle");
        assertTrue(middle.isPresent());
        assertEquals("last", middle.get(), "Faulty next item");
    }

    @Test
    void getNextItem_nextItemUnavailable_nextItemNotFound() {
        assertFalse(dataView.getNextItem("last").isPresent(),
                "Got next item for last item");
    }

    @Test
    void getPrevItem_prevItemAvailable_prevItemFound() {
        Optional<String> middle = dataView.getPreviousItem("middle");
        assertTrue(middle.isPresent());
        assertEquals("first", middle.get(),
                "Item in middle should have previous item");
    }

    @Test
    void getPrevItem_prevItemUnavailable_prevItemNotFound() {
        assertFalse(dataView.getPreviousItem("first").isPresent(),
                "Got previous item for first index");
    }

    @Test
    void setFilter_filterIsSet_filteredItemsObtained() {
        assertEquals(items.size(), dataView.getItemCount());
        dataView.setFilter(item -> item.equals("first"));
        assertEquals(1, dataView.getItemCount(),
                "Filter was not applied to data size");
        assertEquals("first", dataView.getItems().findFirst().get(),
                "Expected item is missing from filtered data");
    }

    @Test
    void setFilter_filterIsSetAndDropped_allItemsRefreshed() {
        dataView.setFilter(item -> item.equals("first"));
        assertEquals(1, ((ListDataViewImpl) dataView).getRefreshCount());
        dataView.removeFilters();
        assertEquals(2, ((ListDataViewImpl) dataView).getRefreshCount());
        dataView.addFilter(ignored -> true);
        assertEquals(3, ((ListDataViewImpl) dataView).getRefreshCount());
    }

    @Test
    void setFilter_resetFilterWithDataView_dataProviderFilterNotAffected() {
        dataProvider.setFilter(item -> item.equals("first"));
        dataView.setFilter(null);
        assertEquals(1, dataView.getItemCount(),
                "Filter reset in data view impacts the data provider filter");
        assertArrayEquals(new String[] { "first" },
                dataView.getItems().toArray(),
                "Filter reset in data view impacts the data provider filter");
    }

    @Test
    void setSortComparator_sortIsSet_sortedItemsObtained() {
        dataView.setSortComparator(String::compareTo);
        assertEquals("first,last,middle",
                dataView.getItems().collect(Collectors.joining(",")),
                "Unexpected data set order after comparator setup");
    }

    @Test
    void setSortComparator_sortIsSet_sortedItemsRefreshed() {
        dataView.setSortComparator(String::compareTo);
        assertEquals(1, ((ListDataViewImpl) dataView).getRefreshCount());
        dataView.removeSorting();
        assertEquals(2, ((ListDataViewImpl) dataView).getRefreshCount());
        dataView.addSortComparator(String::compareTo);
        assertEquals(3, ((ListDataViewImpl) dataView).getRefreshCount());
        dataView.setSortOrder(ValueProvider.identity(),
                SortDirection.ASCENDING);
        assertEquals(4, ((ListDataViewImpl) dataView).getRefreshCount());
        dataView.addSortOrder(ValueProvider.identity(),
                SortDirection.DESCENDING);
        assertEquals(5, ((ListDataViewImpl) dataView).getRefreshCount());
    }

    @Test
    void setSortComparator_resetSortingWithDataView_dataProviderSortingNotAffected() {
        dataProvider.setSortComparator(String::compareTo);
        dataView.setSortComparator(null);
        assertArrayEquals(new String[] { "first", "last", "middle" },
                dataView.getItems().toArray(),
                "Sorting reset in data view impacts the sorting in data provider");
    }

    @Test
    void addSortComparator_twoComparatorsAdded_itemsSortedByCompositeComparator() {
        dataProvider = DataProvider.ofItems("b3", "a2", "a1");
        dataView = new ListDataViewImpl(() -> dataProvider, component);
        dataView.addSortComparator((s1, s2) -> Character.valueOf(s1.charAt(0))
                .compareTo(Character.valueOf(s2.charAt(0))));
        assertEquals("a2,a1,b3",
                dataView.getItems().collect(Collectors.joining(",")),
                "Unexpected data set order (comparator 1)");
        dataView.addSortComparator((s1, s2) -> Character.valueOf(s1.charAt(1))
                .compareTo(Character.valueOf(s2.charAt(1))));
        assertEquals("a1,a2,b3",
                dataView.getItems().collect(Collectors.joining(",")),
                "Unexpected data set order (comparator 2)");
    }

    @Test
    void setSortOrder_sortOrderIsSet_sortedItemsObtained() {
        dataView.setSortOrder(ValueProvider.identity(),
                SortDirection.ASCENDING);
        assertEquals("first,last,middle",
                dataView.getItems().collect(Collectors.joining(",")),
                "Unexpected data set order");
    }

    @Test
    void addSortOrder_twoOrdersAdded_itemsSortedByCompositeOrders() {
        dataProvider = DataProvider.ofItems("b3", "a1", "a2");
        dataView = new ListDataViewImpl(() -> dataProvider,
                new TestComponent());
        dataView.addSortOrder((item) -> item.charAt(0),
                SortDirection.DESCENDING);
        assertEquals("b3,a1,a2",
                dataView.getItems().collect(Collectors.joining(",")),
                "Unexpected data set order (order 1)");
        dataView.addSortOrder((item) -> item.charAt(1),
                SortDirection.DESCENDING);
        assertEquals("b3,a2,a1",
                dataView.getItems().collect(Collectors.joining(",")),
                "Unexpected data set order (order 2)");
    }

    @Test
    void removeSorting_sortingSetAndThenRemoved_initialSortingObtained() {
        dataView.setSortOrder(ValueProvider.identity(),
                SortDirection.ASCENDING);
        assertEquals("first,last,middle",
                dataView.getItems().collect(Collectors.joining(",")),
                "Unexpected data set order");
        dataView.removeSorting();
        assertEquals("first,middle,last",
                dataView.getItems().collect(Collectors.joining(",")),
                "Unexpected data set order");
    }

    @Test
    void contains_itemPresentedInDataSet_itemFound() {
        assertTrue(dataView.contains("first"),
                "Set item was not found in the data");
    }

    @Test
    void contains_itemNotPresentedInDataSet_itemNotFound() {
        assertFalse(dataView.contains("absent item"),
                "Non existent item found in data");
    }

    @Test
    void setIdentifierProvider_defaultIdentity_equalsIsUsed() {
        assertTrue(beanDataView.contains(new Item(1L, "value1")));
        assertFalse(beanDataView.contains(new Item(1L, "non present")));
        assertFalse(beanDataView.contains(new Item(4L, "value1")));
    }

    @Test
    void setIdentifierProvider_dataProviderIdentity_getIdIsUsed() {
        itemListDataProvider = new AbstractDataViewTest.CustomIdentityItemDataProvider(
                getTestItems());

        assertTrue(beanDataView.contains(new Item(1L, "value1")));
        assertTrue(beanDataView.contains(new Item(1L, "non present")));
        assertFalse(beanDataView.contains(new Item(4L, "value1")));
    }

    @Test
    void setIdentifierProvider_customIdentifierProvider_customIdentifierProviderIsUsed() {
        beanDataView.setIdentifierProvider(Item::getValue);

        assertTrue(beanDataView.contains(new Item(1L, "value1")));
        assertFalse(beanDataView.contains(new Item(1L, "non present")));
        assertTrue(beanDataView.contains(new Item(4L, "value1")));
    }

    @Test
    void setIdentifierProvider_dataProviderHasChanged_newDataProviderIsUsed() {
        assertFalse(beanDataView.contains(new Item(1L, "non present")));

        itemListDataProvider = new AbstractDataViewTest.CustomIdentityItemDataProvider(
                getTestItems());

        assertTrue(beanDataView.contains(new Item(1L, "non present")));

        itemListDataProvider = DataProvider
                .ofItems(new Item(10L, "description10"));

        assertFalse(beanDataView.contains(new Item(1L, "non present")));
        assertTrue(beanDataView.contains(new Item(10L, "description10")));
    }

    @Test
    void setIdentifierProvider_dataProviderHasChanged_identifierProviderRetained() {
        assertFalse(
                beanDataView.contains(new Item(4L, "non present", "descr1")));

        beanDataView.setIdentifierProvider(Item::getDescription);

        assertTrue(
                beanDataView.contains(new Item(4L, "non present", "descr1")));

        itemListDataProvider = new AbstractDataViewTest.CustomIdentityItemDataProvider(
                getTestItems());

        assertTrue(
                beanDataView.contains(new Item(4L, "non present", "descr1")));
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    @Test
    void setIdentifierProvider_firesIdentifierProviderChangeEvent() {
        ComponentEventListener mockEventListener = Mockito
                .mock(ComponentEventListener.class);
        beanDataView.addIdentifierProviderChangeListener(mockEventListener);
        beanDataView.setIdentifierProvider(Item::getId);

        Mockito.verify(mockEventListener, Mockito.times(1))
                .onComponentEvent(Mockito.any());
    }

    @Test
    void addIdentifierProviderChangeListener_doesNotAcceptNull() {
        assertThrows(NullPointerException.class,
                () -> beanDataView.addIdentifierProviderChangeListener(null));
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    @Test()
    void addIdentifierProviderChangeListener_removeListener_listenerIsNotNotified() {
        ComponentEventListener mockEventListener = Mockito
                .mock(ComponentEventListener.class);
        Registration registration = beanDataView
                .addIdentifierProviderChangeListener(mockEventListener);
        registration.remove();
        beanDataView.setIdentifierProvider(Item::getId);

        Mockito.verify(mockEventListener, Mockito.times(0))
                .onComponentEvent(Mockito.any());
    }

    @Test
    void contains_filterApplied_itemFilteredOut() {
        assertTrue(beanDataView.contains(new Item(1L, "value1")));

        beanDataView.setFilter(item -> item.getId() > 1L);

        assertFalse(beanDataView.contains(new Item(1L, "value1")));

        assertTrue(beanDataView.contains(new Item(3L, "value3")));
    }

    @Test
    void addItem_itemInDataset() {
        final String newItem = "new Item";
        dataView.addItem(newItem);

        assertEquals(4, dataView.getItemCount());
        assertTrue(dataView.contains(newItem));
        Optional<String> optionalItem = dataView.getNextItem("last");
        assertTrue(optionalItem.isPresent());
        assertEquals(newItem, optionalItem.get());
    }

    @Test
    void addItem_itemAlreadyInList_notAdded() {
        final String newItem = "first";
        dataView.addItem(newItem);

        assertEquals(3, dataView.getItemCount());
        assertTrue(dataView.contains(newItem));
    }

    @Test
    void removeItem_itemRemovedFromDataset() {
        dataView.removeItem("middle");

        assertEquals(2, dataView.getItemCount());
        assertFalse(dataView.contains("middle"));
        Optional<String> optionalItem = dataView.getNextItem("first");
        assertTrue(optionalItem.isPresent());
        assertEquals("last", optionalItem.get());
    }

    @Test
    void removeItem_notInList_dataSetNotChanged() {
        dataView.removeItem("not present");
        assertEquals(3, dataView.getItemCount());
    }

    @Test
    void addItemBefore_itemIsAddedAtExpectedPosition() {
        dataView.addItemBefore("newItem", "middle");

        assertArrayEquals(new String[] { "first", "newItem", "middle", "last" },
                dataView.getItems().toArray(String[]::new));

        dataView.addItemBefore("second", "first");

        assertArrayEquals(
                new String[] { "second", "first", "newItem", "middle", "last" },
                dataView.getItems().toArray(String[]::new));

    }

    @Test
    void addItemBefore_itemAlreadyInList_itemIsMovedAtExpectedPosition() {
        final String newItem = "newItem";
        dataView.addItem(newItem);

        dataView.addItemBefore("newItem", "middle");

        assertArrayEquals(new String[] { "first", "newItem", "middle", "last" },
                dataView.getItems().toArray(String[]::new));
    }

    @Test
    void addItemBefore_itemAndTargetAreTheSame_itemIsNotAdded() {
        dataView.addItemBefore("newItem", "newItem");

        assertArrayEquals(new String[] { "first", "middle", "last" },
                dataView.getItems().toArray(String[]::new));
    }

    @Test
    void addItemBefore_itemNotInCollection_throwsException() {
        var ex = assertThrows(IllegalArgumentException.class,
                () -> dataView.addItemBefore("newItem", "notExistent"));
        assertTrue(ex.getMessage().contains(
                "Item to insert before is not available in the data"));
    }

    @Test
    void addItemBefore_addItemInFilteredDataSet_itemAddedBeforeTheTarget() {
        dataView.addFilter(item -> !item.equalsIgnoreCase("first"));
        dataView.addItemBefore("newItem", "last");

        assertArrayEquals(new String[] { "middle", "newItem", "last" },
                dataView.getItems().toArray(String[]::new));
    }

    @Test
    void addItemAfter_itemIsAddedAtExpectedPosition() {
        dataView.addItemAfter("newItem", "middle");

        assertArrayEquals(new String[] { "first", "middle", "newItem", "last" },
                dataView.getItems().toArray(String[]::new));

        dataView.addItemAfter("second", "last");

        assertArrayEquals(
                new String[] { "first", "middle", "newItem", "last", "second" },
                dataView.getItems().toArray(String[]::new));

        dataView.addItemAfter("middle", "last");

        assertArrayEquals(
                new String[] { "first", "newItem", "last", "middle", "second" },
                dataView.getItems().toArray(String[]::new));
    }

    @Test
    void addItemAfter_itemAlreadyInList_itemIsMovedAtExpectedPosition() {
        final String newItem = "newItem";
        dataView.addItem(newItem);

        dataView.addItemAfter("newItem", "middle");

        assertArrayEquals(new String[] { "first", "middle", "newItem", "last" },
                dataView.getItems().toArray(String[]::new));
    }

    @Test
    void addItemAfter_itemAndTargetAreTheSame_itemIsNotAdded() {
        dataView.addItemAfter("newItem", "newItem");

        assertArrayEquals(new String[] { "first", "middle", "last" },
                dataView.getItems().toArray(String[]::new));
    }

    @Test
    void addItemAfter_itemNotInCollection_throwsException() {
        var ex = assertThrows(IllegalArgumentException.class,
                () -> dataView.addItemAfter("newItem", "notExistent"));
        assertTrue(ex.getMessage()
                .contains("Item to insert after is not available in the data"));
    }

    @Test
    void addItemAfter_addItemInFilteredDataSet_itemAddedAfterTheTarget() {
        dataView.addFilter(item -> !item.equalsIgnoreCase("first"));
        dataView.addItemAfter("newItem", "last");

        assertArrayEquals(new String[] { "middle", "last", "newItem" },
                dataView.getItems().toArray(String[]::new));
    }

    @Test
    void addItems_allItemsAreAdded() {
        dataView.addItems(Arrays.asList("newOne", "newTwo", "newThree"));

        assertArrayEquals(
                new String[] { "first", "middle", "last", "newOne", "newTwo",
                        "newThree" },
                dataView.getItems().toArray(String[]::new));
    }

    @Test
    void addItems_someItemsAlreadyInList_allItemsAreMovedAtTheEndAndOrdered() {
        dataView.addItems(Arrays.asList("first", "newOne", "newTwo"));

        assertArrayEquals(
                new String[] { "middle", "last", "first", "newOne", "newTwo" },
                dataView.getItems().toArray(String[]::new));
    }

    @Test
    void addItems_nullCollectionPassed_throwsException() {
        var ex = assertThrows(NullPointerException.class,
                () -> dataView.addItems(null));
        assertTrue(ex.getMessage().contains("Items collection cannot be null"));
    }

    @Test
    void addItems_emptyCollectionPassed_dataNotChanged() {
        dataView.addItems(Collections.emptyList());
        assertArrayEquals(new String[] { "first", "middle", "last" },
                dataView.getItems().toArray(String[]::new));
    }

    @Test
    void addItemsAfter_allItemsAreAddedAfterTargetItem() {
        dataView.addItemsAfter(Arrays.asList("newOne", "newTwo", "newThree"),
                "first");

        assertArrayEquals(
                new String[] { "first", "newOne", "newTwo", "newThree",
                        "middle", "last" },
                dataView.getItems().toArray(String[]::new));
    }

    @Test
    void addItemsAfter_someItemsAlreadyInList_allItemsAreMovedAfterTargetAndOrdered() {
        dataView.addItemsAfter(Arrays.asList("middle", "newOne", "newTwo"),
                "first");

        assertArrayEquals(
                new String[] { "first", "middle", "newOne", "newTwo", "last" },
                dataView.getItems().toArray(String[]::new));

        dataView.addItemsAfter(Collections.singletonList("newThree"), "last");

        assertArrayEquals(
                new String[] { "first", "middle", "newOne", "newTwo", "last",
                        "newThree" },
                dataView.getItems().toArray(String[]::new));

        dataView.addItemsAfter(Arrays.asList("newFour", "newThree"),
                "newThree");

        assertArrayEquals(
                new String[] { "first", "middle", "newOne", "newTwo", "last",
                        "newFour", "newThree" },
                dataView.getItems().toArray(String[]::new));

        dataView.addItemsAfter(Arrays.asList("newFive", "first"), "first");

        assertArrayEquals(
                new String[] { "newFive", "first", "middle", "newOne", "newTwo",
                        "last", "newFour", "newThree" },
                dataView.getItems().toArray(String[]::new));
    }

    @Test
    void addItemsAfter_targetItemNotInCollection_throwsException() {
        var ex = assertThrows(IllegalArgumentException.class,
                () -> dataView.addItemsAfter(
                        Collections.singletonList("newItem"), "notExistent"));
        assertTrue(ex.getMessage()
                .contains("Item to insert after is not available in the data"));
    }

    @Test
    void addItemsAfter_nullCollectionPassed_throwsException() {
        var ex = assertThrows(NullPointerException.class,
                () -> dataView.addItemsAfter(null, "any"));
        assertTrue(ex.getMessage().contains("Items collection cannot be null"));
    }

    @Test
    void addItemsAfter_emptyCollectionPassed_dataNotChanged() {
        dataView.addItemsAfter(Collections.emptyList(), "any");
        assertArrayEquals(new String[] { "first", "middle", "last" },
                dataView.getItems().toArray(String[]::new));
    }

    @Test
    void addItemsAfter_addItemsInFilteredDataSet_itemsAddedAfterTheTarget() {
        dataView.addFilter(item -> !item.equalsIgnoreCase("first"));
        dataView.addItemsAfter(Arrays.asList("newItem1", "newItem2"), "last");

        assertArrayEquals(
                new String[] { "middle", "last", "newItem1", "newItem2" },
                dataView.getItems().toArray(String[]::new));
    }

    @Test
    void addItemsBefore_allItemsAreAddedBeforeTargetItem() {
        dataView.addItemsBefore(Arrays.asList("newOne", "newTwo", "newThree"),
                "middle");

        assertArrayEquals(
                new String[] { "first", "newOne", "newTwo", "newThree",
                        "middle", "last" },
                dataView.getItems().toArray(String[]::new));
    }

    @Test
    void addItemsBefore_someItemsAlreadyInList_allItemsAreMovedBeforeTargetAndOrdered() {

        dataView.addItemsBefore(Arrays.asList("first", "newOne", "newTwo"),
                "last");

        assertArrayEquals(
                new String[] { "middle", "first", "newOne", "newTwo", "last" },
                dataView.getItems().toArray(String[]::new));

        dataView.addItemsBefore(Arrays.asList("newThree", "last"), "last");

        assertArrayEquals(
                new String[] { "middle", "first", "newOne", "newTwo",
                        "newThree", "last" },
                dataView.getItems().toArray(String[]::new));

        dataView.addItemsBefore(Arrays.asList("newFour", "middle"), "middle");

        assertArrayEquals(
                new String[] { "newFour", "middle", "first", "newOne", "newTwo",
                        "newThree", "last" },
                dataView.getItems().toArray(String[]::new));

        dataView.addItemsBefore(Collections.singletonList("newFive"),
                "newFour");

        assertArrayEquals(
                new String[] { "newFive", "newFour", "middle", "first",
                        "newOne", "newTwo", "newThree", "last" },
                dataView.getItems().toArray(String[]::new));
    }

    @Test
    void addItemsBefore_targetItemNotInCollection_throwsException() {
        var ex = assertThrows(IllegalArgumentException.class,
                () -> dataView.addItemsBefore(
                        Collections.singletonList("newItem"), "notExistent"));
        assertTrue(ex.getMessage().contains(
                "Item to insert before is not available in the data"));
    }

    @Test
    void addItemsBefore_nullCollectionPassed_throwsException() {
        var ex = assertThrows(NullPointerException.class,
                () -> dataView.addItemsBefore(null, "any"));
        assertTrue(ex.getMessage().contains("Items collection cannot be null"));
    }

    @Test
    void addItemsBefore_emptyCollectionPassed_dataNotChanged() {
        dataView.addItemsBefore(Collections.emptyList(), "any");
        assertArrayEquals(new String[] { "first", "middle", "last" },
                dataView.getItems().toArray(String[]::new));
    }

    @Test
    void addItemsBefore_addItemsInFilteredDataSet_itemsAddedBeforeTheTarget() {
        dataView.addFilter(item -> !item.equalsIgnoreCase("first"));
        dataView.addItemsBefore(Arrays.asList("newItem1", "newItem2"), "last");

        assertArrayEquals(
                new String[] { "middle", "newItem1", "newItem2", "last" },
                dataView.getItems().toArray(String[]::new));
    }

    @Test
    void removeItems_itemsOutOfOrder_allItemsAreRemoved() {
        dataView.removeItems(Arrays.asList("middle", "first"));

        assertArrayEquals(new String[] { "last" },
                dataView.getItems().toArray(String[]::new));
    }

    @Test
    void removeItems_nullCollectionPassed_throwsException() {
        var ex = assertThrows(NullPointerException.class,
                () -> dataView.removeItems(null));
        assertTrue(ex.getMessage().contains("Items collection cannot be null"));
    }

    @Test
    void removeItems_emptyCollectionPassed_dataNotChanged() {
        dataView.removeItems(Collections.emptyList());
        assertArrayEquals(new String[] { "first", "middle", "last" },
                dataView.getItems().toArray(String[]::new));
    }

    @Test
    void setItems_nullCollectionPassed_throwsException() {
        var ex = assertThrows(NullPointerException.class,
                () -> dataView.setItems(null));
        assertTrue(ex.getMessage().contains("Items collection cannot be null"));
    }

    @Test
    void setItems_emptyCollectionPassed_dataEmpty() {
        dataView.setItems(Collections.emptyList());
        assertTrue(dataView.getItems().toList().isEmpty());
    }

    @Test
    void setItems_collectionPassed_dataFilled() {
        dataView.setItems(List.of("first", "middle", "last"));
        assertArrayEquals(new String[] { "first", "middle", "last" },
                dataView.getItems().toArray(String[]::new));
    }

    @Test
    void addItemsAndRemoveItems_noConcurrencyIssues() {
        dataView.addItemsBefore(Arrays.asList("newOne", "newTwo", "newThree"),
                "middle");

        assertArrayEquals(
                new String[] { "first", "newOne", "newTwo", "newThree",
                        "middle", "last" },
                dataView.getItems().toArray(String[]::new));

        dataView.removeItems(Arrays.asList("middle", "first"));

        assertArrayEquals(
                new String[] { "newOne", "newTwo", "newThree", "last" },
                dataView.getItems().toArray(String[]::new));

        dataView.addItemsAfter(Arrays.asList("one", "two"), "newOne");

        assertArrayEquals(new String[] { "newOne", "one", "two", "newTwo",
                "newThree", "last" },
                dataView.getItems().toArray(String[]::new));

    }

    @Test
    void dataProviderOnSet_exceptionThrownForAddItemBefore() {
        Set<String> items = new HashSet<>();
        items.add("item1");
        items.add("item2");

        final ListDataProvider<String> stringListDataProvider = new ListDataProvider<>(
                items);
        dataView = new ListDataViewImpl(() -> stringListDataProvider,
                component);

        var ex = assertThrows(IllegalArgumentException.class,
                () -> dataView.addItemBefore("newItem", "item2"));
        assertTrue(ex.getMessage()
                .contains("DataProvider collection 'HashSet' is not a list."));
    }

    @Test
    void dataProviderOnSet_exceptionThrownForAddItemAfter() {
        Set<String> items = new HashSet<>();
        items.add("item1");
        items.add("item2");

        final ListDataProvider<String> stringListDataProvider = new ListDataProvider<>(
                items);
        dataView = new ListDataViewImpl(() -> stringListDataProvider, null);

        var ex = assertThrows(IllegalArgumentException.class,
                () -> dataView.addItemAfter("newItem", "item1"));
        assertTrue(ex.getMessage()
                .contains("DataProvider collection 'HashSet' is not a list."));
    }

    @Test
    void dataProviderOnSet_exceptionThrownForAddItemsAfter() {
        Set<String> items = new HashSet<>();
        items.add("item1");
        items.add("item2");

        final ListDataProvider<String> stringListDataProvider = new ListDataProvider<>(
                items);
        dataView = new ListDataViewImpl(() -> stringListDataProvider,
                component);

        var ex = assertThrows(IllegalArgumentException.class, () -> dataView
                .addItemsAfter(Collections.singleton("newItem"), "item1"));
        assertTrue(ex.getMessage()
                .contains("DataProvider collection 'HashSet' is not a list."));
    }

    @Test
    void dataProviderOnSet_exceptionThrownForAddItemsBefore() {
        Set<String> items = new HashSet<>();
        items.add("item1");
        items.add("item2");

        final ListDataProvider<String> stringListDataProvider = new ListDataProvider<>(
                items);
        dataView = new ListDataViewImpl(() -> stringListDataProvider,
                component);

        var ex = assertThrows(IllegalArgumentException.class, () -> dataView
                .addItemsBefore(Collections.singleton("newItem"), "item1"));
        assertTrue(ex.getMessage()
                .contains("DataProvider collection 'HashSet' is not a list."));
    }

    @Test
    void addFilter_FilterIsAddedOnTop() {
        items = new ArrayList<>(
                Arrays.asList("item1", "item2", "item22", "item3"));
        dataProvider = DataProvider.ofCollection(items);

        assertEquals(4, dataView.getItems().count());

        dataView.addFilter(item -> item.equals("item1") || item.equals("item2")
                || item.equals("item22"));

        assertEquals(3, dataView.getItems().count());

        dataView.addFilter(item -> item.endsWith("2"));

        assertEquals(2, dataView.getItems().count());
    }

    @Test
    void removeFilters_removesAllSetAndAddedFilters() {
        items = new ArrayList<>(
                Arrays.asList("item1", "item2", "item22", "item3"));
        dataProvider = DataProvider.ofCollection(items);

        dataView.setFilter(item -> item.endsWith("2") || item.endsWith("3"));

        assertEquals(3, dataView.getItems().count(), "Set filter not applied");

        dataView.addFilter(item -> item.endsWith("2"));

        assertEquals(2, dataView.getItems().count(),
                "Added filter not applied");

        dataView.removeFilters();

        assertEquals(4, dataView.getItems().count(),
                "Filters were not cleared");
    }

    @Test
    void refreshItem_itemPresentInDataSet_refreshesItem() {
        Collection<Item> items = getTestItems();

        ListDataProvider<Item> dataProvider = Mockito
                .spy(DataProvider.ofCollection(items));

        ItemListDataView dataView = new ItemListDataView(() -> dataProvider,
                component);

        Iterator<Item> iterator = items.iterator();
        Item firstItem = iterator.next();
        firstItem.setValue("updatedValue");

        dataView.refreshItem(firstItem);
        Mockito.verify(dataProvider).refreshItem(firstItem);

        dataView.setIdentifierProvider(Item::getId);
        Item secondItem = iterator.next();
        secondItem.setValue("updatedValue");

        Item secondItemDuplicate = new Item(2L);
        dataView.refreshItem(secondItemDuplicate);
        // Verify that the refresh is made on a new object, no on an old
        // object.
        Mockito.verify(dataProvider).refreshItem(secondItemDuplicate);
        Mockito.verify(dataProvider, Mockito.times(0)).refreshItem(secondItem);
    }

    @Test
    void getItem_correctIndex_itemFound() {
        assertEquals("first", dataView.getItem(0),
                "Wrong item returned for index");
    }

    @Test
    void getItem_negativeIndex_throwsException() {
        var ex = assertThrows(IndexOutOfBoundsException.class,
                () -> dataView.getItem(-1));
        assertTrue(ex.getMessage().contains(
                "Given index -1 is outside of the accepted range '0 - 2'"));
    }

    @Test
    void getItem_emptyDataSet_throwsException() {
        dataProvider = DataProvider.ofItems();
        var ex = assertThrows(IndexOutOfBoundsException.class,
                () -> dataView.getItem(0));
        assertTrue(
                ex.getMessage().contains("Requested index 0 on empty data."));
    }

    @Test
    void getItem_filteringApplied_itemFound() {
        assertEquals("middle", dataView.getItem(1),
                "Wrong item returned for index");
        dataProvider
                .setFilter(item -> "first".equals(item) || "last".equals(item));
        assertEquals("last", dataView.getItem(1),
                "Wrong item returned for index");
    }

    @Test
    void getItem_sortingApplied_itemFound() {
        assertEquals("first", dataView.getItem(0),
                "Wrong item returned for index");
        dataProvider.setSortOrder(item -> item, SortDirection.DESCENDING);
        assertEquals("middle", dataView.getItem(0),
                "Wrong item returned for index");
    }

    @Test
    void getItem_indexOutsideOfSize_throwsException() {
        assertThrows(IndexOutOfBoundsException.class,
                () -> dataView.getItem(items.size()));
    }

    @Test
    void getItemIndex_itemPresentedInDataSet_indexFound() {
        assertEquals(Optional.of(1), dataView.getItemIndex("middle"),
                "Wrong index returned for item");
    }

    @Test
    void getItemIndex_itemNotPresentedInDataSet_indexNotFound() {
        assertEquals(Optional.empty(), dataView.getItemIndex("notPresent"),
                "Wrong index returned for item");
    }

    @Test
    void getItemIndex_filteringApplied_indexFound() {
        dataProvider
                .setFilter(item -> "first".equals(item) || "last".equals(item));
        assertEquals(Optional.of(1), dataView.getItemIndex("last"),
                "Wrong index returned for item");
    }

    @Test
    void getItemIndex_sortingApplied_indexFound() {
        dataProvider.setSortOrder(item -> item, SortDirection.DESCENDING);
        assertEquals(Optional.of(0), dataView.getItemIndex("middle"),
                "Wrong index returned for item");
    }

    @Test
    void getItemIndex_itemNotPresentedInDataSet_filteringApplied_indexNotFound() {
        dataProvider
                .setFilter(item -> "first".equals(item) || "last".equals(item));
        assertEquals(Optional.empty(), dataView.getItemIndex("middle"),
                "Wrong index returned for item");
    }

    @Test
    void dataViewCreatedAndAPIUsed_beforeSettingDataProvider_verificationPassed() {
        // Data provider verification should pass even if the developer
        // hasn't setup any data provider to a component. In the example
        // below, we just create a data communicator instance but don't call
        // 'setDataProvider' method.
        DataCommunicator<String> dataCommunicator = new DataCommunicator<>(
                (item, jsonObject) -> {
                }, null, null, component.getElement().getNode());

        AbstractListDataView<String> dataView = new AbstractListDataView<String>(
                dataCommunicator::getDataProvider, component,
                (filter, sorting) -> {
                }) {
        };

        // Check that we can add a listener even if not data provider set by
        // user
        dataView.addItemCountChangeListener(event -> {
        });

        // Check that the verification is still passed during data view API
        // usage, because the default data provider is an in-memory one
        dataView.addItem("foo");
    }

    @Test
    void createListDataProviderFromArrayOfItems_addingOneItem_itemCountShouldBeIncreasedByOne() {
        ListDataProvider<Item> localListDataProvider = DataProvider
                .ofItems(new Item(1L, "First"), new Item(2L, "Second"));

        ListDataView<Item, AbstractListDataView<Item>> listDataView = new ItemListDataView(
                () -> localListDataProvider, component);

        long itemCount = listDataView.getItemCount();

        listDataView.addItem(new Item(3L, "Third"));
        assertEquals(itemCount + 1, listDataView.getItemCount());
    }

    @Test
    void createListDataProviderFromArrayOfItems_removingOneItem_itemCountShouldBeDecreasedByOne() {
        Item first = new Item(1L, "First");
        Item second = new Item(2L, "Second");
        ListDataProvider<Item> localListDataProvider = DataProvider
                .ofItems(first, second);

        ListDataView<Item, AbstractListDataView<Item>> listDataView = new ItemListDataView(
                () -> localListDataProvider, component);

        long itemCount = listDataView.getItemCount();

        listDataView.removeItem(first);
        assertEquals(itemCount - 1, listDataView.getItemCount());
    }

    @Test
    void setFilter_twoComponentsHasSameDataProvider_onlyTargetComponentImpacted() {
        TestComponent component1 = new TestComponent();
        TestComponent component2 = new TestComponent();

        ListDataViewImpl listDataView1 = new ListDataViewImpl(
                () -> dataProvider, component1);

        ListDataViewImpl listDataView2 = new ListDataViewImpl(
                () -> dataProvider, component2);

        assertEquals(3, listDataView1.getItemCount(),
                "Unexpected initial items count for component #1");

        assertEquals(3, listDataView2.getItemCount(),
                "Unexpected initial items count for component #2");

        listDataView1.setFilter(
                item -> "middle".equals(item) || "last".equals(item));

        assertNull(dataProvider.getFilter(),
                "Unexpected delegation of filtering to data provider");

        assertEquals(2, listDataView1.getItemCount(),
                "Unexpected component #1 items count after filter apply");

        assertEquals(3, listDataView2.getItemCount(),
                "Unexpected component #2 items count after filter apply to component #1");

        assertArrayEquals(new String[] { "middle", "last" },
                listDataView1.getItems().toArray(),
                "Unexpected items after filter apply");

        assertArrayEquals(new String[] { "first", "middle", "last" },
                listDataView2.getItems().toArray(),
                "Unexpected items after filter apply");

        listDataView1.addFilter("middle"::equals);

        assertEquals(1, listDataView1.getItemCount(),
                "Unexpected component #1 items count after filter apply");

        assertEquals(3, listDataView2.getItemCount(),
                "Unexpected component #2 items count after filter apply to component #1");

        assertArrayEquals(new String[] { "middle" },
                listDataView1.getItems().toArray(),
                "Unexpected items after filter apply");

        assertArrayEquals(new String[] { "first", "middle", "last" },
                listDataView2.getItems().toArray(),
                "Unexpected items after filter apply");

        listDataView1.removeFilters();

        assertEquals(3, listDataView1.getItemCount(),
                "Unexpected component #1 items count after filter remove");

        assertEquals(3, listDataView2.getItemCount(),
                "Unexpected component #2 items count after filter remove in component #1");

        assertArrayEquals(new String[] { "first", "middle", "last" },
                listDataView1.getItems().toArray(),
                "Unexpected items after filter remove");

        assertArrayEquals(new String[] { "first", "middle", "last" },
                listDataView2.getItems().toArray(),
                "Unexpected items after filter remove");
    }

    @Test
    void setFilter_setDataProviderFilter_bothDataViewAndDataProviderFilterAreApplied() {
        TestComponent component = new TestComponent();

        ListDataViewImpl listDataView = new ListDataViewImpl(() -> dataProvider,
                component);

        listDataView.setFilter(
                item -> "middle".equals(item) || "last".equals(item));

        dataProvider.setFilter("middle"::equals);

        assertArrayEquals(new String[] { "middle" },
                listDataView.getItems().toArray(),
                "Unexpected items after applying filter to both component"
                        + " and data provider");

        dataProvider.clearFilters();

        assertArrayEquals(new String[] { "middle", "last" },
                listDataView.getItems().toArray(),
                "Unexpected items after clearing data " + "provider's filter");
    }

    @Test
    void setSortComparator_twoComponentsHasSameDataProvider_onlyTargetComponentImpacted() {
        TestComponent component1 = new TestComponent();
        TestComponent component2 = new TestComponent();

        ListDataProvider<Item> dataProvider = DataProvider.ofItems(
                new Item(1L, "baz"), new Item(2L, "foo"), new Item(1L, "bar"));

        ItemListDataView listDataView1 = new ItemListDataView(
                () -> dataProvider, component1);

        ItemListDataView listDataView2 = new ItemListDataView(
                () -> dataProvider, component2);

        listDataView1.setSortComparator(
                (item1, item2) -> Long.compare(item1.getId(), item2.getId()));

        assertNull(dataProvider.getSortComparator(),
                "Unexpected delegation of sorting to data provider");

        assertArrayEquals(new Long[] { 1L, 1L, 2L },
                listDataView1.getItems().map(Item::getId).toArray(),
                "Unexpected items sorting for component #1");

        assertArrayEquals(new Long[] { 1L, 2L, 1L },
                listDataView2.getItems().map(Item::getId).toArray(),
                "Unexpected items sorting for component #2");

        listDataView1.addSortComparator((item1, item2) -> item1.getValue()
                .compareToIgnoreCase(item2.getValue()));

        assertArrayEquals(new String[] { "bar", "baz", "foo" },
                listDataView1.getItems().map(Item::getValue).toArray(),
                "Unexpected items sorting for component #1");

        assertArrayEquals(new String[] { "baz", "foo", "bar" },
                listDataView2.getItems().map(Item::getValue).toArray(),
                "Unexpected items sorting for component #2");

        listDataView1.removeSorting();

        assertArrayEquals(
                new Item[] { new Item(1L, "baz"), new Item(2L, "foo"),
                        new Item(1L, "bar") },
                listDataView1.getItems().toArray(),
                "Unexpected items sorting for component #1");

        assertArrayEquals(
                new Item[] { new Item(1L, "baz"), new Item(2L, "foo"),
                        new Item(1L, "bar") },
                listDataView2.getItems().toArray(),
                "Unexpected items sorting for component #2");
    }

    @Test
    void setSortOrder_twoComponentsHasSameDataProvider_onlyTargetComponentImpacted() {
        TestComponent component1 = new TestComponent();
        TestComponent component2 = new TestComponent();

        ListDataProvider<Item> dataProvider = DataProvider.ofItems(
                new Item(1L, "baz"), new Item(2L, "foo"), new Item(1L, "bar"));

        ItemListDataView listDataView1 = new ItemListDataView(
                () -> dataProvider, component1);

        ItemListDataView listDataView2 = new ItemListDataView(
                () -> dataProvider, component2);

        listDataView1.setSortOrder(Item::getId, SortDirection.ASCENDING);

        assertArrayEquals(new Long[] { 1L, 1L, 2L },
                listDataView1.getItems().map(Item::getId).toArray(),
                "Unexpected items sorting for component #1");

        assertArrayEquals(new Long[] { 1L, 2L, 1L },
                listDataView2.getItems().map(Item::getId).toArray(),
                "Unexpected items sorting for component #2");

        listDataView1.addSortOrder(Item::getValue, SortDirection.ASCENDING);

        assertArrayEquals(new String[] { "bar", "baz", "foo" },
                listDataView1.getItems().map(Item::getValue).toArray(),
                "Unexpected items sorting for component #1");

        assertArrayEquals(new String[] { "baz", "foo", "bar" },
                listDataView2.getItems().map(Item::getValue).toArray(),
                "Unexpected items sorting for component #2");

        listDataView1.removeSorting();

        assertArrayEquals(
                new Item[] { new Item(1L, "baz"), new Item(2L, "foo"),
                        new Item(1L, "bar") },
                listDataView1.getItems().toArray(),
                "Unexpected items sorting for component #1");

        assertArrayEquals(
                new Item[] { new Item(1L, "baz"), new Item(2L, "foo"),
                        new Item(1L, "bar") },
                listDataView2.getItems().toArray(),
                "Unexpected items sorting for component #2");
    }

    @Test
    void setSortComparator_setDataProviderSorting_bothDataViewAndDataProviderSortingAreApplied() {
        TestComponent component = new TestComponent();

        ListDataProvider<Item> dataProvider = DataProvider.ofItems(
                new Item(2L, "bar"), new Item(3L, "foo"), new Item(1L, "bar"));

        ItemListDataView listDataView = new ItemListDataView(() -> dataProvider,
                component);

        listDataView.setSortOrder(Item::getValue, SortDirection.ASCENDING);

        assertArrayEquals(
                new Item[] { new Item(2L, "bar"), new Item(1L, "bar"),
                        new Item(3L, "foo") },
                listDataView.getItems().toArray(),
                "Unexpected items after applying filter to both component"
                        + " and data provider");

        dataProvider.setSortOrder(Item::getId, SortDirection.ASCENDING);

        assertArrayEquals(
                new Item[] { new Item(1L, "bar"), new Item(2L, "bar"),
                        new Item(3L, "foo") },
                listDataView.getItems().toArray(),
                "Unexpected items after applying filter to both component"
                        + " and data provider");

        dataProvider.setSortComparator(null);

        assertArrayEquals(
                new Item[] { new Item(2L, "bar"), new Item(1L, "bar"),
                        new Item(3L, "foo") },
                listDataView.getItems().toArray(),
                "Unexpected items after applying filter to both component"
                        + " and data provider");
    }

    @Test
    void setFilterOrSorting_filterOrSortingUpdated_filterOrSortingChangedCallbackInvoked() {
        Collection<String> items = Arrays.asList("foo", "bar", "baz");

        AtomicReference<SerializablePredicate<String>> filtering = new AtomicReference<>();
        AtomicReference<SerializableComparator<String>> sorting = new AtomicReference<>();

        ListDataViewImpl listDataView = new ListDataViewImpl(() -> dataProvider,
                component, (filter, sort) -> {
                    filtering.set(filter);
                    sorting.set(sort);
                });

        listDataView.setFilter("bar"::equals);

        assertNotNull(filtering.get());
        assertArrayEquals(new String[] { "bar" },
                items.stream().filter(filtering.get()).toArray());
        assertNull(sorting.get());

        listDataView.removeFilters();
        listDataView.setSortOrder(String::toLowerCase, SortDirection.ASCENDING);
        assertNotNull(sorting.get());
        assertNull(filtering.get());
        assertArrayEquals(new String[] { "bar", "baz", "foo" },
                items.stream().sorted(sorting.get()).toArray());

        listDataView.setSortComparator(null);
        assertNull(sorting.get());
    }

    @Test
    void filterOrSortingChangedCallback_emptyCallbackProvided_throws() {
        var ex = assertThrows(NullPointerException.class,
                () -> new ListDataViewImpl(() -> dataProvider, component,
                        null));
        assertTrue(ex.getMessage()
                .contains("Filter or Sorting Change Callback cannot be empty"));
    }

    @Test
    void addFilter_serialize_dataViewSerializable() throws Throwable {
        DataViewUtils.setComponentFilter(component, term -> true);
        ListDataProvider<String> dp = DataProvider.ofCollection(Set.of("A"));
        ListDataViewImpl listDataView = new ListDataViewImpl(() -> dp,
                component, (filter, sort) -> {
                });
        listDataView.addFilter(term -> true);
        ListDataViewImpl out = serializeAndDeserialize(listDataView);
        assertNotNull(out);
    }

    private static class ListDataViewImpl extends AbstractListDataView<String> {

        private final AtomicInteger refreshCount = new AtomicInteger(0);

        public ListDataViewImpl(
                SerializableSupplier<? extends DataProvider<String, ?>> dataProviderSupplier,
                Component component) {
            this(dataProviderSupplier, component, (filter, sorting) -> {
                // no-op for test purposes
            });
        }

        public ListDataViewImpl(
                SerializableSupplier<? extends DataProvider<String, ?>> dataProviderSupplier,
                Component component,
                SerializableBiConsumer<SerializablePredicate<String>, SerializableComparator<String>> filterOrSortingChangedCallback) {
            super(dataProviderSupplier, component,
                    filterOrSortingChangedCallback);
        }

        @Override
        public void refreshAll() {
            super.refreshAll();
            refreshCount.incrementAndGet();
        }

        public int getRefreshCount() {
            return refreshCount.get();
        }
    }

    private static class ItemListDataView extends AbstractListDataView<Item> {

        public ItemListDataView(
                SerializableSupplier<DataProvider<Item, ?>> dataProviderSupplier,
                Component component) {
            super(dataProviderSupplier, component, (filter, sorting) -> {
                // no-op for test purposes
            });
        }

        public ItemListDataView(
                SerializableSupplier<? extends DataProvider<Item, ?>> dataProviderSupplier,
                Component component,
                SerializableBiConsumer<SerializablePredicate<Item>, SerializableComparator<Item>> filterOrSortingChangedCallback) {
            super(dataProviderSupplier, component,
                    filterOrSortingChangedCallback);
        }
    }

    @Test
    void getItems_withOffsetAndLimit_subsetReturned() {
        // items: first, middle, last
        Stream<String> stream = dataView.getItems(1, 1);
        assertEquals("middle", stream.findFirst().orElse(null));
    }

    @Test
    void getItems_withOffsetAndLimit_largerLimit_returnsAvailable() {
        // items: first, middle, last
        Stream<String> stream = dataView.getItems(1, 10);
        List<String> list = stream.collect(Collectors.toList());
        assertEquals(2, list.size());
        assertEquals("middle", list.get(0));
        assertEquals("last", list.get(1));
    }

    private Collection<Item> getTestItems() {
        return new ArrayList<>(Arrays.asList(new Item(1L, "value1", "descr1"),
                new Item(2L, "value2", "descr2"),
                new Item(3L, "value3", "descr3")));
    }

    @Tag("test-component")
    private static class TestComponent extends Component {
    }

    @Test
    void getItems_withNegativeOffset_throwsException() {
        IndexOutOfBoundsException exception = assertThrows(
                IndexOutOfBoundsException.class,
                () -> dataView.getItems(-1, 10));
        assertEquals("Offset must be non-negative", exception.getMessage());
    }

    @Test
    void getItems_withNegativeLimit_throwsException() {
        IndexOutOfBoundsException exception = assertThrows(
                IndexOutOfBoundsException.class,
                () -> dataView.getItems(0, -1));
        assertEquals("Limit must be non-negative", exception.getMessage());
    }
}
