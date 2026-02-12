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

import org.junit.jupiter.api.Assertions;
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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AbstractListDataViewTest {

    private Collection<String> items;

    private ListDataProvider<String> dataProvider;

    private AbstractListDataView<String> dataView;

    private Component component;

    private AbstractListDataView<Item> beanDataView;
    private ListDataProvider<Item> itemListDataProvider;

    @BeforeEach
    public void init() {
        items = new ArrayList<>(Arrays.asList("first", "middle", "last"));
        dataProvider = DataProvider.ofCollection(items);
        component = new TestComponent();
        dataView = new ListDataViewImpl(() -> dataProvider, component);

        itemListDataProvider = DataProvider.ofCollection(getTestItems());
        beanDataView = new ItemListDataView(() -> itemListDataProvider,
                component);
    }

    @Test
    public void createListDataViewInstance_faultyDataProvider_throwsException() {
        DataProvider dataProvider = DataProvider
                .fromCallbacks(query -> Stream.of("one"), query -> 1);
        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> new ListDataViewImpl(() -> dataProvider, null));
        Assertions.assertTrue(ex.getMessage()
                .contains("ListDataViewImpl only supports 'ListDataProvider' "
                        + "or it's subclasses, but was given a "
                        + "'AbstractBackEndDataProvider'"));
    }

    @Test
    public void getItemCount_noFiltersSet_dataSizeObtained() {
        Assertions.assertEquals(items.size(), dataView.getItemCount(),
                "Unexpected item count");
    }

    @Test
    public void getItemCount_filtersSet_filteredItemsObtained() {
        dataProvider.setFilter(item -> item.equals("first"));
        Assertions.assertEquals(1, dataView.getItemCount(),
                "Unexpected item count");
    }

    @Test
    public void getNextItem_nextItemAvailable_nextItemFound() {
        Optional<String> middle = dataView.getNextItem("middle");
        Assertions.assertTrue(middle.isPresent());
        Assertions.assertEquals("last", middle.get(), "Faulty next item");
    }

    @Test
    public void getNextItem_nextItemUnavailable_nextItemNotFound() {
        Assertions.assertFalse(dataView.getNextItem("last").isPresent(),
                "Got next item for last item");
    }

    @Test
    public void getPrevItem_prevItemAvailable_prevItemFound() {
        Optional<String> middle = dataView.getPreviousItem("middle");
        Assertions.assertTrue(middle.isPresent());
        Assertions.assertEquals("first", middle.get(),
                "Item in middle should have previous item");
    }

    @Test
    public void getPrevItem_prevItemUnavailable_prevItemNotFound() {
        Assertions.assertFalse(dataView.getPreviousItem("first").isPresent(),
                "Got previous item for first index");
    }

    @Test
    public void setFilter_filterIsSet_filteredItemsObtained() {
        Assertions.assertEquals(items.size(), dataView.getItemCount());
        dataView.setFilter(item -> item.equals("first"));
        Assertions.assertEquals(1, dataView.getItemCount(),
                "Filter was not applied to data size");
        Assertions.assertEquals("first", dataView.getItems().findFirst().get(),
                "Expected item is missing from filtered data");
    }

    @Test
    public void setFilter_filterIsSetAndDropped_allItemsRefreshed() {
        dataView.setFilter(item -> item.equals("first"));
        Assertions.assertEquals(1,
                ((ListDataViewImpl) dataView).getRefreshCount());
        dataView.removeFilters();
        Assertions.assertEquals(2,
                ((ListDataViewImpl) dataView).getRefreshCount());
        dataView.addFilter(ignored -> true);
        Assertions.assertEquals(3,
                ((ListDataViewImpl) dataView).getRefreshCount());
    }

    @Test
    public void setFilter_resetFilterWithDataView_dataProviderFilterNotAffected() {
        dataProvider.setFilter(item -> item.equals("first"));
        dataView.setFilter(null);
        Assertions.assertEquals(1, dataView.getItemCount(),
                "Filter reset in data view impacts the data provider filter");
        Assertions.assertArrayEquals(new String[] { "first" },
                dataView.getItems().toArray(),
                "Filter reset in data view impacts the data provider filter");
    }

    @Test
    public void setSortComparator_sortIsSet_sortedItemsObtained() {
        dataView.setSortComparator(String::compareTo);
        Assertions.assertEquals("first,last,middle",
                dataView.getItems().collect(Collectors.joining(",")),
                "Unexpected data set order after comparator setup");
    }

    @Test
    public void setSortComparator_sortIsSet_sortedItemsRefreshed() {
        dataView.setSortComparator(String::compareTo);
        Assertions.assertEquals(1,
                ((ListDataViewImpl) dataView).getRefreshCount());
        dataView.removeSorting();
        Assertions.assertEquals(2,
                ((ListDataViewImpl) dataView).getRefreshCount());
        dataView.addSortComparator(String::compareTo);
        Assertions.assertEquals(3,
                ((ListDataViewImpl) dataView).getRefreshCount());
        dataView.setSortOrder(ValueProvider.identity(),
                SortDirection.ASCENDING);
        Assertions.assertEquals(4,
                ((ListDataViewImpl) dataView).getRefreshCount());
        dataView.addSortOrder(ValueProvider.identity(),
                SortDirection.DESCENDING);
        Assertions.assertEquals(5,
                ((ListDataViewImpl) dataView).getRefreshCount());
    }

    @Test
    public void setSortComparator_resetSortingWithDataView_dataProviderSortingNotAffected() {
        dataProvider.setSortComparator(String::compareTo);
        dataView.setSortComparator(null);
        Assertions.assertArrayEquals(new String[] { "first", "last", "middle" },
                dataView.getItems().toArray(),
                "Sorting reset in data view impacts the sorting in data provider");
    }

    @Test
    public void addSortComparator_twoComparatorsAdded_itemsSortedByCompositeComparator() {
        dataProvider = DataProvider.ofItems("b3", "a2", "a1");
        dataView = new ListDataViewImpl(() -> dataProvider, component);
        dataView.addSortComparator((s1, s2) -> Character.valueOf(s1.charAt(0))
                .compareTo(Character.valueOf(s2.charAt(0))));
        Assertions.assertEquals("a2,a1,b3",
                dataView.getItems().collect(Collectors.joining(",")),
                "Unexpected data set order (comparator 1)");
        dataView.addSortComparator((s1, s2) -> Character.valueOf(s1.charAt(1))
                .compareTo(Character.valueOf(s2.charAt(1))));
        Assertions.assertEquals("a1,a2,b3",
                dataView.getItems().collect(Collectors.joining(",")),
                "Unexpected data set order (comparator 2)");
    }

    @Test
    public void setSortOrder_sortOrderIsSet_sortedItemsObtained() {
        dataView.setSortOrder(ValueProvider.identity(),
                SortDirection.ASCENDING);
        Assertions.assertEquals("first,last,middle",
                dataView.getItems().collect(Collectors.joining(",")),
                "Unexpected data set order");
    }

    @Test
    public void addSortOrder_twoOrdersAdded_itemsSortedByCompositeOrders() {
        dataProvider = DataProvider.ofItems("b3", "a1", "a2");
        dataView = new ListDataViewImpl(() -> dataProvider,
                new TestComponent());
        dataView.addSortOrder((item) -> item.charAt(0),
                SortDirection.DESCENDING);
        Assertions.assertEquals("b3,a1,a2",
                dataView.getItems().collect(Collectors.joining(",")),
                "Unexpected data set order (order 1)");
        dataView.addSortOrder((item) -> item.charAt(1),
                SortDirection.DESCENDING);
        Assertions.assertEquals("b3,a2,a1",
                dataView.getItems().collect(Collectors.joining(",")),
                "Unexpected data set order (order 2)");
    }

    @Test
    public void removeSorting_sortingSetAndThenRemoved_initialSortingObtained() {
        dataView.setSortOrder(ValueProvider.identity(),
                SortDirection.ASCENDING);
        Assertions.assertEquals("first,last,middle",
                dataView.getItems().collect(Collectors.joining(",")),
                "Unexpected data set order");
        dataView.removeSorting();
        Assertions.assertEquals("first,middle,last",
                dataView.getItems().collect(Collectors.joining(",")),
                "Unexpected data set order");
    }

    @Test
    public void contains_itemPresentedInDataSet_itemFound() {
        Assertions.assertTrue(dataView.contains("first"),
                "Set item was not found in the data");
    }

    @Test
    public void contains_itemNotPresentedInDataSet_itemNotFound() {
        Assertions.assertFalse(dataView.contains("absent item"),
                "Non existent item found in data");
    }

    @Test
    public void setIdentifierProvider_defaultIdentity_equalsIsUsed() {
        Assertions.assertTrue(beanDataView.contains(new Item(1L, "value1")));
        Assertions.assertFalse(
                beanDataView.contains(new Item(1L, "non present")));
        Assertions.assertFalse(beanDataView.contains(new Item(4L, "value1")));
    }

    @Test
    public void setIdentifierProvider_dataProviderIdentity_getIdIsUsed() {
        itemListDataProvider = new AbstractDataViewTest.CustomIdentityItemDataProvider(
                getTestItems());

        Assertions.assertTrue(beanDataView.contains(new Item(1L, "value1")));
        Assertions
                .assertTrue(beanDataView.contains(new Item(1L, "non present")));
        Assertions.assertFalse(beanDataView.contains(new Item(4L, "value1")));
    }

    @Test
    public void setIdentifierProvider_customIdentifierProvider_customIdentifierProviderIsUsed() {
        beanDataView.setIdentifierProvider(Item::getValue);

        Assertions.assertTrue(beanDataView.contains(new Item(1L, "value1")));
        Assertions.assertFalse(
                beanDataView.contains(new Item(1L, "non present")));
        Assertions.assertTrue(beanDataView.contains(new Item(4L, "value1")));
    }

    @Test
    public void setIdentifierProvider_dataProviderHasChanged_newDataProviderIsUsed() {
        Assertions.assertFalse(
                beanDataView.contains(new Item(1L, "non present")));

        itemListDataProvider = new AbstractDataViewTest.CustomIdentityItemDataProvider(
                getTestItems());

        Assertions
                .assertTrue(beanDataView.contains(new Item(1L, "non present")));

        itemListDataProvider = DataProvider
                .ofItems(new Item(10L, "description10"));

        Assertions.assertFalse(
                beanDataView.contains(new Item(1L, "non present")));
        Assertions.assertTrue(
                beanDataView.contains(new Item(10L, "description10")));
    }

    @Test
    public void setIdentifierProvider_dataProviderHasChanged_identifierProviderRetained() {
        Assertions.assertFalse(
                beanDataView.contains(new Item(4L, "non present", "descr1")));

        beanDataView.setIdentifierProvider(Item::getDescription);

        Assertions.assertTrue(
                beanDataView.contains(new Item(4L, "non present", "descr1")));

        itemListDataProvider = new AbstractDataViewTest.CustomIdentityItemDataProvider(
                getTestItems());

        Assertions.assertTrue(
                beanDataView.contains(new Item(4L, "non present", "descr1")));
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    @Test
    public void setIdentifierProvider_firesIdentifierProviderChangeEvent() {
        ComponentEventListener mockEventListener = Mockito
                .mock(ComponentEventListener.class);
        beanDataView.addIdentifierProviderChangeListener(mockEventListener);
        beanDataView.setIdentifierProvider(Item::getId);

        Mockito.verify(mockEventListener, Mockito.times(1))
                .onComponentEvent(Mockito.any());
    }

    @Test
    public void addIdentifierProviderChangeListener_doesNotAcceptNull() {
        assertThrows(NullPointerException.class, () -> {
            beanDataView.addIdentifierProviderChangeListener(null);
        });
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    @Test()
    public void addIdentifierProviderChangeListener_removeListener_listenerIsNotNotified() {
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
    public void contains_filterApplied_itemFilteredOut() {
        Assertions.assertTrue(beanDataView.contains(new Item(1L, "value1")));

        beanDataView.setFilter(item -> item.getId() > 1L);

        Assertions.assertFalse(beanDataView.contains(new Item(1L, "value1")));

        Assertions.assertTrue(beanDataView.contains(new Item(3L, "value3")));
    }

    @Test
    public void addItem_itemInDataset() {
        final String newItem = "new Item";
        dataView.addItem(newItem);

        Assertions.assertEquals(4, dataView.getItemCount());
        Assertions.assertTrue(dataView.contains(newItem));
        Optional<String> optionalItem = dataView.getNextItem("last");
        Assertions.assertTrue(optionalItem.isPresent());
        Assertions.assertEquals(newItem, optionalItem.get());
    }

    @Test
    public void addItem_itemAlreadyInList_notAdded() {
        final String newItem = "first";
        dataView.addItem(newItem);

        Assertions.assertEquals(3, dataView.getItemCount());
        Assertions.assertTrue(dataView.contains(newItem));
    }

    @Test
    public void removeItem_itemRemovedFromDataset() {
        dataView.removeItem("middle");

        Assertions.assertEquals(2, dataView.getItemCount());
        Assertions.assertFalse(dataView.contains("middle"));
        Optional<String> optionalItem = dataView.getNextItem("first");
        Assertions.assertTrue(optionalItem.isPresent());
        Assertions.assertEquals("last", optionalItem.get());
    }

    @Test
    public void removeItem_notInList_dataSetNotChanged() {
        dataView.removeItem("not present");
        Assertions.assertEquals(3, dataView.getItemCount());
    }

    @Test
    public void addItemBefore_itemIsAddedAtExpectedPosition() {
        dataView.addItemBefore("newItem", "middle");

        Assertions.assertArrayEquals(
                new String[] { "first", "newItem", "middle", "last" },
                dataView.getItems().toArray(String[]::new));

        dataView.addItemBefore("second", "first");

        Assertions.assertArrayEquals(
                new String[] { "second", "first", "newItem", "middle", "last" },
                dataView.getItems().toArray(String[]::new));

    }

    @Test
    public void addItemBefore_itemAlreadyInList_itemIsMovedAtExpectedPosition() {
        final String newItem = "newItem";
        dataView.addItem(newItem);

        dataView.addItemBefore("newItem", "middle");

        Assertions.assertArrayEquals(
                new String[] { "first", "newItem", "middle", "last" },
                dataView.getItems().toArray(String[]::new));
    }

    @Test
    public void addItemBefore_itemAndTargetAreTheSame_itemIsNotAdded() {
        dataView.addItemBefore("newItem", "newItem");

        Assertions.assertArrayEquals(new String[] { "first", "middle", "last" },
                dataView.getItems().toArray(String[]::new));
    }

    @Test
    public void addItemBefore_itemNotInCollection_throwsException() {
        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> dataView.addItemBefore("newItem", "notExistent"));
        Assertions.assertTrue(ex.getMessage().contains(
                "Item to insert before is not available in the data"));
    }

    @Test
    public void addItemBefore_addItemInFilteredDataSet_itemAddedBeforeTheTarget() {
        dataView.addFilter(item -> !item.equalsIgnoreCase("first"));
        dataView.addItemBefore("newItem", "last");

        Assertions.assertArrayEquals(
                new String[] { "middle", "newItem", "last" },
                dataView.getItems().toArray(String[]::new));
    }

    @Test
    public void addItemAfter_itemIsAddedAtExpectedPosition() {
        dataView.addItemAfter("newItem", "middle");

        Assertions.assertArrayEquals(
                new String[] { "first", "middle", "newItem", "last" },
                dataView.getItems().toArray(String[]::new));

        dataView.addItemAfter("second", "last");

        Assertions.assertArrayEquals(
                new String[] { "first", "middle", "newItem", "last", "second" },
                dataView.getItems().toArray(String[]::new));

        dataView.addItemAfter("middle", "last");

        Assertions.assertArrayEquals(
                new String[] { "first", "newItem", "last", "middle", "second" },
                dataView.getItems().toArray(String[]::new));
    }

    @Test
    public void addItemAfter_itemAlreadyInList_itemIsMovedAtExpectedPosition() {
        final String newItem = "newItem";
        dataView.addItem(newItem);

        dataView.addItemAfter("newItem", "middle");

        Assertions.assertArrayEquals(
                new String[] { "first", "middle", "newItem", "last" },
                dataView.getItems().toArray(String[]::new));
    }

    @Test
    public void addItemAfter_itemAndTargetAreTheSame_itemIsNotAdded() {
        dataView.addItemAfter("newItem", "newItem");

        Assertions.assertArrayEquals(new String[] { "first", "middle", "last" },
                dataView.getItems().toArray(String[]::new));
    }

    @Test
    public void addItemAfter_itemNotInCollection_throwsException() {
        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> dataView.addItemAfter("newItem", "notExistent"));
        Assertions.assertTrue(ex.getMessage()
                .contains("Item to insert after is not available in the data"));
    }

    @Test
    public void addItemAfter_addItemInFilteredDataSet_itemAddedAfterTheTarget() {
        dataView.addFilter(item -> !item.equalsIgnoreCase("first"));
        dataView.addItemAfter("newItem", "last");

        Assertions.assertArrayEquals(
                new String[] { "middle", "last", "newItem" },
                dataView.getItems().toArray(String[]::new));
    }

    @Test
    public void addItems_allItemsAreAdded() {
        dataView.addItems(Arrays.asList("newOne", "newTwo", "newThree"));

        Assertions.assertArrayEquals(
                new String[] { "first", "middle", "last", "newOne", "newTwo",
                        "newThree" },
                dataView.getItems().toArray(String[]::new));
    }

    @Test
    public void addItems_someItemsAlreadyInList_allItemsAreMovedAtTheEndAndOrdered() {
        dataView.addItems(Arrays.asList("first", "newOne", "newTwo"));

        Assertions.assertArrayEquals(
                new String[] { "middle", "last", "first", "newOne", "newTwo" },
                dataView.getItems().toArray(String[]::new));
    }

    @Test
    public void addItems_nullCollectionPassed_throwsException() {
        NullPointerException ex = assertThrows(NullPointerException.class,
                () -> dataView.addItems(null));
        Assertions.assertTrue(
                ex.getMessage().contains("Items collection cannot be null"));
    }

    @Test
    public void addItems_emptyCollectionPassed_dataNotChanged() {
        dataView.addItems(Collections.emptyList());
        Assertions.assertArrayEquals(new String[] { "first", "middle", "last" },
                dataView.getItems().toArray(String[]::new));
    }

    @Test
    public void addItemsAfter_allItemsAreAddedAfterTargetItem() {
        dataView.addItemsAfter(Arrays.asList("newOne", "newTwo", "newThree"),
                "first");

        Assertions.assertArrayEquals(
                new String[] { "first", "newOne", "newTwo", "newThree",
                        "middle", "last" },
                dataView.getItems().toArray(String[]::new));
    }

    @Test
    public void addItemsAfter_someItemsAlreadyInList_allItemsAreMovedAfterTargetAndOrdered() {
        dataView.addItemsAfter(Arrays.asList("middle", "newOne", "newTwo"),
                "first");

        Assertions.assertArrayEquals(
                new String[] { "first", "middle", "newOne", "newTwo", "last" },
                dataView.getItems().toArray(String[]::new));

        dataView.addItemsAfter(Collections.singletonList("newThree"), "last");

        Assertions.assertArrayEquals(
                new String[] { "first", "middle", "newOne", "newTwo", "last",
                        "newThree" },
                dataView.getItems().toArray(String[]::new));

        dataView.addItemsAfter(Arrays.asList("newFour", "newThree"),
                "newThree");

        Assertions.assertArrayEquals(
                new String[] { "first", "middle", "newOne", "newTwo", "last",
                        "newFour", "newThree" },
                dataView.getItems().toArray(String[]::new));

        dataView.addItemsAfter(Arrays.asList("newFive", "first"), "first");

        Assertions.assertArrayEquals(
                new String[] { "newFive", "first", "middle", "newOne", "newTwo",
                        "last", "newFour", "newThree" },
                dataView.getItems().toArray(String[]::new));
    }

    @Test
    public void addItemsAfter_targetItemNotInCollection_throwsException() {
        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class, () -> dataView.addItemsAfter(
                        Collections.singletonList("newItem"), "notExistent"));
        Assertions.assertTrue(ex.getMessage()
                .contains("Item to insert after is not available in the data"));
    }

    @Test
    public void addItemsAfter_nullCollectionPassed_throwsException() {
        NullPointerException ex = assertThrows(NullPointerException.class,
                () -> dataView.addItemsAfter(null, "any"));
        Assertions.assertTrue(
                ex.getMessage().contains("Items collection cannot be null"));
    }

    @Test
    public void addItemsAfter_emptyCollectionPassed_dataNotChanged() {
        dataView.addItemsAfter(Collections.emptyList(), "any");
        Assertions.assertArrayEquals(new String[] { "first", "middle", "last" },
                dataView.getItems().toArray(String[]::new));
    }

    @Test
    public void addItemsAfter_addItemsInFilteredDataSet_itemsAddedAfterTheTarget() {
        dataView.addFilter(item -> !item.equalsIgnoreCase("first"));
        dataView.addItemsAfter(Arrays.asList("newItem1", "newItem2"), "last");

        Assertions.assertArrayEquals(
                new String[] { "middle", "last", "newItem1", "newItem2" },
                dataView.getItems().toArray(String[]::new));
    }

    @Test
    public void addItemsBefore_allItemsAreAddedBeforeTargetItem() {
        dataView.addItemsBefore(Arrays.asList("newOne", "newTwo", "newThree"),
                "middle");

        Assertions.assertArrayEquals(
                new String[] { "first", "newOne", "newTwo", "newThree",
                        "middle", "last" },
                dataView.getItems().toArray(String[]::new));
    }

    @Test
    public void addItemsBefore_someItemsAlreadyInList_allItemsAreMovedBeforeTargetAndOrdered() {

        dataView.addItemsBefore(Arrays.asList("first", "newOne", "newTwo"),
                "last");

        Assertions.assertArrayEquals(
                new String[] { "middle", "first", "newOne", "newTwo", "last" },
                dataView.getItems().toArray(String[]::new));

        dataView.addItemsBefore(Arrays.asList("newThree", "last"), "last");

        Assertions.assertArrayEquals(
                new String[] { "middle", "first", "newOne", "newTwo",
                        "newThree", "last" },
                dataView.getItems().toArray(String[]::new));

        dataView.addItemsBefore(Arrays.asList("newFour", "middle"), "middle");

        Assertions.assertArrayEquals(
                new String[] { "newFour", "middle", "first", "newOne", "newTwo",
                        "newThree", "last" },
                dataView.getItems().toArray(String[]::new));

        dataView.addItemsBefore(Collections.singletonList("newFive"),
                "newFour");

        Assertions.assertArrayEquals(
                new String[] { "newFive", "newFour", "middle", "first",
                        "newOne", "newTwo", "newThree", "last" },
                dataView.getItems().toArray(String[]::new));
    }

    @Test
    public void addItemsBefore_targetItemNotInCollection_throwsException() {
        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class, () -> dataView.addItemsBefore(
                        Collections.singletonList("newItem"), "notExistent"));
        Assertions.assertTrue(ex.getMessage().contains(
                "Item to insert before is not available in the data"));
    }

    @Test
    public void addItemsBefore_nullCollectionPassed_throwsException() {
        NullPointerException ex = assertThrows(NullPointerException.class,
                () -> dataView.addItemsBefore(null, "any"));
        Assertions.assertTrue(
                ex.getMessage().contains("Items collection cannot be null"));
    }

    @Test
    public void addItemsBefore_emptyCollectionPassed_dataNotChanged() {
        dataView.addItemsBefore(Collections.emptyList(), "any");
        Assertions.assertArrayEquals(new String[] { "first", "middle", "last" },
                dataView.getItems().toArray(String[]::new));
    }

    @Test
    public void addItemsBefore_addItemsInFilteredDataSet_itemsAddedBeforeTheTarget() {
        dataView.addFilter(item -> !item.equalsIgnoreCase("first"));
        dataView.addItemsBefore(Arrays.asList("newItem1", "newItem2"), "last");

        Assertions.assertArrayEquals(
                new String[] { "middle", "newItem1", "newItem2", "last" },
                dataView.getItems().toArray(String[]::new));
    }

    @Test
    public void removeItems_itemsOutOfOrder_allItemsAreRemoved() {
        dataView.removeItems(Arrays.asList("middle", "first"));

        Assertions.assertArrayEquals(new String[] { "last" },
                dataView.getItems().toArray(String[]::new));
    }

    @Test
    public void removeItems_nullCollectionPassed_throwsException() {
        NullPointerException ex = assertThrows(NullPointerException.class,
                () -> dataView.removeItems(null));
        Assertions.assertTrue(
                ex.getMessage().contains("Items collection cannot be null"));
    }

    @Test
    public void removeItems_emptyCollectionPassed_dataNotChanged() {
        dataView.removeItems(Collections.emptyList());
        Assertions.assertArrayEquals(new String[] { "first", "middle", "last" },
                dataView.getItems().toArray(String[]::new));
    }

    @Test
    public void setItems_nullCollectionPassed_throwsException() {
        NullPointerException ex = assertThrows(NullPointerException.class,
                () -> dataView.setItems(null));
        Assertions.assertTrue(
                ex.getMessage().contains("Items collection cannot be null"));
    }

    @Test
    public void setItems_emptyCollectionPassed_dataEmpty() {
        dataView.setItems(Collections.emptyList());
        Assertions.assertTrue(dataView.getItems().toList().isEmpty());
    }

    @Test
    public void setItems_collectionPassed_dataFilled() {
        dataView.setItems(List.of("first", "middle", "last"));
        Assertions.assertArrayEquals(new String[] { "first", "middle", "last" },
                dataView.getItems().toArray(String[]::new));
    }

    @Test
    public void addItemsAndRemoveItems_noConcurrencyIssues() {
        dataView.addItemsBefore(Arrays.asList("newOne", "newTwo", "newThree"),
                "middle");

        Assertions.assertArrayEquals(
                new String[] { "first", "newOne", "newTwo", "newThree",
                        "middle", "last" },
                dataView.getItems().toArray(String[]::new));

        dataView.removeItems(Arrays.asList("middle", "first"));

        Assertions.assertArrayEquals(
                new String[] { "newOne", "newTwo", "newThree", "last" },
                dataView.getItems().toArray(String[]::new));

        dataView.addItemsAfter(Arrays.asList("one", "two"), "newOne");

        Assertions
                .assertArrayEquals(
                        new String[] { "newOne", "one", "two", "newTwo",
                                "newThree", "last" },
                        dataView.getItems().toArray(String[]::new));

    }

    @Test
    public void dataProviderOnSet_exceptionThrownForAddItemBefore() {
        Set<String> items = new HashSet<>();
        items.add("item1");
        items.add("item2");

        final ListDataProvider<String> stringListDataProvider = new ListDataProvider<>(
                items);
        dataView = new ListDataViewImpl(() -> stringListDataProvider,
                component);

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> dataView.addItemBefore("newItem", "item2"));
        Assertions.assertTrue(ex.getMessage()
                .contains("DataProvider collection 'HashSet' is not a list."));
    }

    @Test
    public void dataProviderOnSet_exceptionThrownForAddItemAfter() {
        Set<String> items = new HashSet<>();
        items.add("item1");
        items.add("item2");

        final ListDataProvider<String> stringListDataProvider = new ListDataProvider<>(
                items);
        dataView = new ListDataViewImpl(() -> stringListDataProvider, null);

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> dataView.addItemAfter("newItem", "item1"));
        Assertions.assertTrue(ex.getMessage()
                .contains("DataProvider collection 'HashSet' is not a list."));
    }

    @Test
    public void dataProviderOnSet_exceptionThrownForAddItemsAfter() {
        Set<String> items = new HashSet<>();
        items.add("item1");
        items.add("item2");

        final ListDataProvider<String> stringListDataProvider = new ListDataProvider<>(
                items);
        dataView = new ListDataViewImpl(() -> stringListDataProvider,
                component);

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> dataView.addItemsAfter(Collections.singleton("newItem"),
                        "item1"));
        Assertions.assertTrue(ex.getMessage()
                .contains("DataProvider collection 'HashSet' is not a list."));
    }

    @Test
    public void dataProviderOnSet_exceptionThrownForAddItemsBefore() {
        Set<String> items = new HashSet<>();
        items.add("item1");
        items.add("item2");

        final ListDataProvider<String> stringListDataProvider = new ListDataProvider<>(
                items);
        dataView = new ListDataViewImpl(() -> stringListDataProvider,
                component);

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> dataView.addItemsBefore(Collections.singleton("newItem"),
                        "item1"));
        Assertions.assertTrue(ex.getMessage()
                .contains("DataProvider collection 'HashSet' is not a list."));
    }

    @Test
    public void addFilter_FilterIsAddedOnTop() {
        items = new ArrayList<>(
                Arrays.asList("item1", "item2", "item22", "item3"));
        dataProvider = DataProvider.ofCollection(items);

        Assertions.assertEquals(4, dataView.getItems().count());

        dataView.addFilter(item -> item.equals("item1") || item.equals("item2")
                || item.equals("item22"));

        Assertions.assertEquals(3, dataView.getItems().count());

        dataView.addFilter(item -> item.endsWith("2"));

        Assertions.assertEquals(2, dataView.getItems().count());
    }

    @Test
    public void removeFilters_removesAllSetAndAddedFilters() {
        items = new ArrayList<>(
                Arrays.asList("item1", "item2", "item22", "item3"));
        dataProvider = DataProvider.ofCollection(items);

        dataView.setFilter(item -> item.endsWith("2") || item.endsWith("3"));

        Assertions.assertEquals(3, dataView.getItems().count(),
                "Set filter not applied");

        dataView.addFilter(item -> item.endsWith("2"));

        Assertions.assertEquals(2, dataView.getItems().count(),
                "Added filter not applied");

        dataView.removeFilters();

        Assertions.assertEquals(4, dataView.getItems().count(),
                "Filters were not cleared");
    }

    @Test
    public void refreshItem_itemPresentInDataSet_refreshesItem() {
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
    public void getItem_correctIndex_itemFound() {
        Assertions.assertEquals("first", dataView.getItem(0),
                "Wrong item returned for index");
    }

    @Test
    public void getItem_negativeIndex_throwsException() {
        IndexOutOfBoundsException ex = assertThrows(
                IndexOutOfBoundsException.class, () -> dataView.getItem(-1));
        Assertions.assertTrue(ex.getMessage().contains(
                "Given index -1 is outside of the accepted range '0 - 2'"));
    }

    @Test
    public void getItem_emptyDataSet_throwsException() {
        dataProvider = DataProvider.ofItems();
        IndexOutOfBoundsException ex = assertThrows(
                IndexOutOfBoundsException.class, () -> dataView.getItem(0));
        Assertions.assertTrue(
                ex.getMessage().contains("Requested index 0 on empty data."));
    }

    @Test
    public void getItem_filteringApplied_itemFound() {
        Assertions.assertEquals("middle", dataView.getItem(1),
                "Wrong item returned for index");
        dataProvider
                .setFilter(item -> "first".equals(item) || "last".equals(item));
        Assertions.assertEquals("last", dataView.getItem(1),
                "Wrong item returned for index");
    }

    @Test
    public void getItem_sortingApplied_itemFound() {
        Assertions.assertEquals("first", dataView.getItem(0),
                "Wrong item returned for index");
        dataProvider.setSortOrder(item -> item, SortDirection.DESCENDING);
        Assertions.assertEquals("middle", dataView.getItem(0),
                "Wrong item returned for index");
    }

    @Test
    public void getItem_indexOutsideOfSize_throwsException() {
        assertThrows(IndexOutOfBoundsException.class,
                () -> dataView.getItem(items.size()));
    }

    @Test
    public void getItemIndex_itemPresentedInDataSet_indexFound() {
        Assertions.assertEquals(Optional.of(1), dataView.getItemIndex("middle"),
                "Wrong index returned for item");
    }

    @Test
    public void getItemIndex_itemNotPresentedInDataSet_indexNotFound() {
        Assertions.assertEquals(Optional.empty(),
                dataView.getItemIndex("notPresent"),
                "Wrong index returned for item");
    }

    @Test
    public void getItemIndex_filteringApplied_indexFound() {
        dataProvider
                .setFilter(item -> "first".equals(item) || "last".equals(item));
        Assertions.assertEquals(Optional.of(1), dataView.getItemIndex("last"),
                "Wrong index returned for item");
    }

    @Test
    public void getItemIndex_sortingApplied_indexFound() {
        dataProvider.setSortOrder(item -> item, SortDirection.DESCENDING);
        Assertions.assertEquals(Optional.of(0), dataView.getItemIndex("middle"),
                "Wrong index returned for item");
    }

    @Test
    public void getItemIndex_itemNotPresentedInDataSet_filteringApplied_indexNotFound() {
        dataProvider
                .setFilter(item -> "first".equals(item) || "last".equals(item));
        Assertions.assertEquals(Optional.empty(),
                dataView.getItemIndex("middle"),
                "Wrong index returned for item");
    }

    @Test
    public void dataViewCreatedAndAPIUsed_beforeSettingDataProvider_verificationPassed() {
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
    public void createListDataProviderFromArrayOfItems_addingOneItem_itemCountShouldBeIncreasedByOne() {
        ListDataProvider<Item> localListDataProvider = DataProvider
                .ofItems(new Item(1L, "First"), new Item(2L, "Second"));

        ListDataView<Item, AbstractListDataView<Item>> listDataView = new ItemListDataView(
                () -> localListDataProvider, component);

        long itemCount = listDataView.getItemCount();

        listDataView.addItem(new Item(3L, "Third"));
        Assertions.assertEquals(itemCount + 1, listDataView.getItemCount());
    }

    @Test
    public void createListDataProviderFromArrayOfItems_removingOneItem_itemCountShouldBeDecreasedByOne() {
        Item first = new Item(1L, "First");
        Item second = new Item(2L, "Second");
        ListDataProvider<Item> localListDataProvider = DataProvider
                .ofItems(first, second);

        ListDataView<Item, AbstractListDataView<Item>> listDataView = new ItemListDataView(
                () -> localListDataProvider, component);

        long itemCount = listDataView.getItemCount();

        listDataView.removeItem(first);
        Assertions.assertEquals(itemCount - 1, listDataView.getItemCount());
    }

    @Test
    public void setFilter_twoComponentsHasSameDataProvider_onlyTargetComponentImpacted() {
        TestComponent component1 = new TestComponent();
        TestComponent component2 = new TestComponent();

        ListDataViewImpl listDataView1 = new ListDataViewImpl(
                () -> dataProvider, component1);

        ListDataViewImpl listDataView2 = new ListDataViewImpl(
                () -> dataProvider, component2);

        Assertions.assertEquals(3, listDataView1.getItemCount(),
                "Unexpected initial items count for component #1");

        Assertions.assertEquals(3, listDataView2.getItemCount(),
                "Unexpected initial items count for component #2");

        listDataView1.setFilter(
                item -> "middle".equals(item) || "last".equals(item));

        Assertions.assertNull(dataProvider.getFilter(),
                "Unexpected delegation of filtering to data provider");

        Assertions.assertEquals(2, listDataView1.getItemCount(),
                "Unexpected component #1 items count after filter apply");

        Assertions.assertEquals(3, listDataView2.getItemCount(),
                "Unexpected component #2 items count after filter apply to component #1");

        Assertions.assertArrayEquals(new String[] { "middle", "last" },
                listDataView1.getItems().toArray(),
                "Unexpected items after filter apply");

        Assertions.assertArrayEquals(new String[] { "first", "middle", "last" },
                listDataView2.getItems().toArray(),
                "Unexpected items after filter apply");

        listDataView1.addFilter("middle"::equals);

        Assertions.assertEquals(1, listDataView1.getItemCount(),
                "Unexpected component #1 items count after filter apply");

        Assertions.assertEquals(3, listDataView2.getItemCount(),
                "Unexpected component #2 items count after filter apply to component #1");

        Assertions.assertArrayEquals(new String[] { "middle" },
                listDataView1.getItems().toArray(),
                "Unexpected items after filter apply");

        Assertions.assertArrayEquals(new String[] { "first", "middle", "last" },
                listDataView2.getItems().toArray(),
                "Unexpected items after filter apply");

        listDataView1.removeFilters();

        Assertions.assertEquals(3, listDataView1.getItemCount(),
                "Unexpected component #1 items count after filter remove");

        Assertions.assertEquals(3, listDataView2.getItemCount(),
                "Unexpected component #2 items count after filter remove in component #1");

        Assertions.assertArrayEquals(new String[] { "first", "middle", "last" },
                listDataView1.getItems().toArray(),
                "Unexpected items after filter remove");

        Assertions.assertArrayEquals(new String[] { "first", "middle", "last" },
                listDataView2.getItems().toArray(),
                "Unexpected items after filter remove");
    }

    @Test
    public void setFilter_setDataProviderFilter_bothDataViewAndDataProviderFilterAreApplied() {
        TestComponent component = new TestComponent();

        ListDataViewImpl listDataView = new ListDataViewImpl(() -> dataProvider,
                component);

        listDataView.setFilter(
                item -> "middle".equals(item) || "last".equals(item));

        dataProvider.setFilter("middle"::equals);

        Assertions.assertArrayEquals(new String[] { "middle" },
                listDataView.getItems().toArray(),
                "Unexpected items after applying filter to both component"
                        + " and data provider");

        dataProvider.clearFilters();

        Assertions.assertArrayEquals(new String[] { "middle", "last" },
                listDataView.getItems().toArray(),
                "Unexpected items after clearing data provider's filter");
    }

    @Test
    public void setSortComparator_twoComponentsHasSameDataProvider_onlyTargetComponentImpacted() {
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

        Assertions.assertNull(dataProvider.getSortComparator(),
                "Unexpected delegation of sorting to data provider");

        Assertions.assertArrayEquals(new Long[] { 1L, 1L, 2L },
                listDataView1.getItems().map(Item::getId).toArray(),
                "Unexpected items sorting for component #1");

        Assertions.assertArrayEquals(new Long[] { 1L, 2L, 1L },
                listDataView2.getItems().map(Item::getId).toArray(),
                "Unexpected items sorting for component #2");

        listDataView1.addSortComparator((item1, item2) -> item1.getValue()
                .compareToIgnoreCase(item2.getValue()));

        Assertions.assertArrayEquals(new String[] { "bar", "baz", "foo" },
                listDataView1.getItems().map(Item::getValue).toArray(),
                "Unexpected items sorting for component #1");

        Assertions.assertArrayEquals(new String[] { "baz", "foo", "bar" },
                listDataView2.getItems().map(Item::getValue).toArray(),
                "Unexpected items sorting for component #2");

        listDataView1.removeSorting();

        Assertions.assertArrayEquals(
                new Item[] { new Item(1L, "baz"), new Item(2L, "foo"),
                        new Item(1L, "bar") },
                listDataView1.getItems().toArray(),
                "Unexpected items sorting for component #1");

        Assertions.assertArrayEquals(
                new Item[] { new Item(1L, "baz"), new Item(2L, "foo"),
                        new Item(1L, "bar") },
                listDataView2.getItems().toArray(),
                "Unexpected items sorting for component #2");
    }

    @Test
    public void setSortOrder_twoComponentsHasSameDataProvider_onlyTargetComponentImpacted() {
        TestComponent component1 = new TestComponent();
        TestComponent component2 = new TestComponent();

        ListDataProvider<Item> dataProvider = DataProvider.ofItems(
                new Item(1L, "baz"), new Item(2L, "foo"), new Item(1L, "bar"));

        ItemListDataView listDataView1 = new ItemListDataView(
                () -> dataProvider, component1);

        ItemListDataView listDataView2 = new ItemListDataView(
                () -> dataProvider, component2);

        listDataView1.setSortOrder(Item::getId, SortDirection.ASCENDING);

        Assertions.assertArrayEquals(new Long[] { 1L, 1L, 2L },
                listDataView1.getItems().map(Item::getId).toArray(),
                "Unexpected items sorting for component #1");

        Assertions.assertArrayEquals(new Long[] { 1L, 2L, 1L },
                listDataView2.getItems().map(Item::getId).toArray(),
                "Unexpected items sorting for component #2");

        listDataView1.addSortOrder(Item::getValue, SortDirection.ASCENDING);

        Assertions.assertArrayEquals(new String[] { "bar", "baz", "foo" },
                listDataView1.getItems().map(Item::getValue).toArray(),
                "Unexpected items sorting for component #1");

        Assertions.assertArrayEquals(new String[] { "baz", "foo", "bar" },
                listDataView2.getItems().map(Item::getValue).toArray(),
                "Unexpected items sorting for component #2");

        listDataView1.removeSorting();

        Assertions.assertArrayEquals(
                new Item[] { new Item(1L, "baz"), new Item(2L, "foo"),
                        new Item(1L, "bar") },
                listDataView1.getItems().toArray(),
                "Unexpected items sorting for component #1");

        Assertions.assertArrayEquals(
                new Item[] { new Item(1L, "baz"), new Item(2L, "foo"),
                        new Item(1L, "bar") },
                listDataView2.getItems().toArray(),
                "Unexpected items sorting for component #2");
    }

    @Test
    public void setSortComparator_setDataProviderSorting_bothDataViewAndDataProviderSortingAreApplied() {
        TestComponent component = new TestComponent();

        ListDataProvider<Item> dataProvider = DataProvider.ofItems(
                new Item(2L, "bar"), new Item(3L, "foo"), new Item(1L, "bar"));

        ItemListDataView listDataView = new ItemListDataView(() -> dataProvider,
                component);

        listDataView.setSortOrder(Item::getValue, SortDirection.ASCENDING);

        Assertions.assertArrayEquals(
                new Item[] { new Item(2L, "bar"), new Item(1L, "bar"),
                        new Item(3L, "foo") },
                listDataView.getItems().toArray(),
                "Unexpected items after applying filter to both component"
                        + " and data provider");

        dataProvider.setSortOrder(Item::getId, SortDirection.ASCENDING);

        Assertions.assertArrayEquals(
                new Item[] { new Item(1L, "bar"), new Item(2L, "bar"),
                        new Item(3L, "foo") },
                listDataView.getItems().toArray(),
                "Unexpected items after applying filter to both component"
                        + " and data provider");

        dataProvider.setSortComparator(null);

        Assertions.assertArrayEquals(
                new Item[] { new Item(2L, "bar"), new Item(1L, "bar"),
                        new Item(3L, "foo") },
                listDataView.getItems().toArray(),
                "Unexpected items after applying filter to both component"
                        + " and data provider");
    }

    @Test
    public void setFilterOrSorting_filterOrSortingUpdated_filterOrSortingChangedCallbackInvoked() {
        Collection<String> items = Arrays.asList("foo", "bar", "baz");

        AtomicReference<SerializablePredicate<String>> filtering = new AtomicReference<>();
        AtomicReference<SerializableComparator<String>> sorting = new AtomicReference<>();

        ListDataViewImpl listDataView = new ListDataViewImpl(() -> dataProvider,
                component, (filter, sort) -> {
                    filtering.set(filter);
                    sorting.set(sort);
                });

        listDataView.setFilter("bar"::equals);

        Assertions.assertNotNull(filtering.get());
        Assertions.assertArrayEquals(new String[] { "bar" },
                items.stream().filter(filtering.get()).toArray());
        Assertions.assertNull(sorting.get());

        listDataView.removeFilters();
        listDataView.setSortOrder(String::toLowerCase, SortDirection.ASCENDING);
        Assertions.assertNotNull(sorting.get());
        Assertions.assertNull(filtering.get());
        Assertions.assertArrayEquals(new String[] { "bar", "baz", "foo" },
                items.stream().sorted(sorting.get()).toArray());

        listDataView.setSortComparator(null);
        Assertions.assertNull(sorting.get());
    }

    @Test
    public void filterOrSortingChangedCallback_emptyCallbackProvided_throws() {
        NullPointerException ex = assertThrows(NullPointerException.class,
                () -> new ListDataViewImpl(() -> dataProvider, component,
                        null));
        Assertions.assertTrue(ex.getMessage()
                .contains("Filter or Sorting Change Callback cannot be empty"));
    }

    @Test
    public void addFilter_serialize_dataViewSerializable() throws Throwable {
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
    public void getItems_withOffsetAndLimit_subsetReturned() {
        // items: first, middle, last
        Stream<String> stream = dataView.getItems(1, 1);
        Assertions.assertEquals("middle", stream.findFirst().orElse(null));
    }

    @Test
    public void getItems_withOffsetAndLimit_largerLimit_returnsAvailable() {
        // items: first, middle, last
        Stream<String> stream = dataView.getItems(1, 10);
        List<String> list = stream.collect(Collectors.toList());
        Assertions.assertEquals(2, list.size());
        Assertions.assertEquals("middle", list.get(0));
        Assertions.assertEquals("last", list.get(1));
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
    public void getItems_withNegativeOffset_throwsException() {
        IndexOutOfBoundsException exception = assertThrows(
                IndexOutOfBoundsException.class,
                () -> dataView.getItems(-1, 10));
        Assertions.assertEquals("Offset must be non-negative",
                exception.getMessage());
    }

    @Test
    public void getItems_withNegativeLimit_throwsException() {
        IndexOutOfBoundsException exception = assertThrows(
                IndexOutOfBoundsException.class,
                () -> dataView.getItems(0, -1));
        Assertions.assertEquals("Limit must be non-negative",
                exception.getMessage());
    }
}
