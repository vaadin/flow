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

import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
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
import static org.junit.Assert.assertNotNull;

public class AbstractListDataViewTest {

    private Collection<String> items;

    @Rule
    public ExpectedException exceptionRule = ExpectedException.none();

    private ListDataProvider<String> dataProvider;

    private AbstractListDataView<String> dataView;

    private Component component;

    private AbstractListDataView<Item> beanDataView;
    private ListDataProvider<Item> itemListDataProvider;

    @Before
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
        exceptionRule.expect(IllegalStateException.class);
        exceptionRule.expectMessage(
                "ListDataViewImpl only supports 'ListDataProvider' "
                        + "or it's subclasses, but was given a "
                        + "'AbstractBackEndDataProvider'");
        new ListDataViewImpl(() -> dataProvider, null);
    }

    @Test
    public void getItemCount_noFiltersSet_dataSizeObtained() {
        Assert.assertEquals("Unexpected item count", items.size(),
                dataView.getItemCount());
    }

    @Test
    public void getItemCount_filtersSet_filteredItemsObtained() {
        dataProvider.setFilter(item -> item.equals("first"));
        Assert.assertEquals("Unexpected item count", 1,
                dataView.getItemCount());
    }

    @Test
    public void getNextItem_nextItemAvailable_nextItemFound() {
        Optional<String> middle = dataView.getNextItem("middle");
        Assert.assertTrue(middle.isPresent());
        Assert.assertEquals("Faulty next item", "last", middle.get());
    }

    @Test
    public void getNextItem_nextItemUnavailable_nextItemNotFound() {
        Assert.assertFalse("Got next item for last item",
                dataView.getNextItem("last").isPresent());
    }

    @Test
    public void getPrevItem_prevItemAvailable_prevItemFound() {
        Optional<String> middle = dataView.getPreviousItem("middle");
        Assert.assertTrue(middle.isPresent());
        Assert.assertEquals("Item in middle should have previous item", "first",
                middle.get());
    }

    @Test
    public void getPrevItem_prevItemUnavailable_prevItemNotFound() {
        Assert.assertFalse("Got previous item for first index",
                dataView.getPreviousItem("first").isPresent());
    }

    @Test
    public void setFilter_filterIsSet_filteredItemsObtained() {
        Assert.assertEquals(items.size(), dataView.getItemCount());
        dataView.setFilter(item -> item.equals("first"));
        Assert.assertEquals("Filter was not applied to data size", 1,
                dataView.getItemCount());
        Assert.assertEquals("Expected item is missing from filtered data",
                "first", dataView.getItems().findFirst().get());
    }

    @Test
    public void setFilter_filterIsSetAndDropped_allItemsRefreshed() {
        dataView.setFilter(item -> item.equals("first"));
        Assert.assertEquals(1, ((ListDataViewImpl) dataView).getRefreshCount());
        dataView.removeFilters();
        Assert.assertEquals(2, ((ListDataViewImpl) dataView).getRefreshCount());
        dataView.addFilter(ignored -> true);
        Assert.assertEquals(3, ((ListDataViewImpl) dataView).getRefreshCount());
    }

    @Test
    public void setFilter_resetFilterWithDataView_dataProviderFilterNotAffected() {
        dataProvider.setFilter(item -> item.equals("first"));
        dataView.setFilter(null);
        Assert.assertEquals(
                "Filter reset in data view impacts the data provider filter", 1,
                dataView.getItemCount());
        Assert.assertArrayEquals(
                "Filter reset in data view impacts the data provider filter",
                new String[] { "first" }, dataView.getItems().toArray());
    }

    @Test
    public void setSortComparator_sortIsSet_sortedItemsObtained() {
        dataView.setSortComparator(String::compareTo);
        Assert.assertEquals("Unexpected data set order after comparator setup",
                "first,last,middle",
                dataView.getItems().collect(Collectors.joining(",")));
    }

    @Test
    public void setSortComparator_sortIsSet_sortedItemsRefreshed() {
        dataView.setSortComparator(String::compareTo);
        Assert.assertEquals(1, ((ListDataViewImpl) dataView).getRefreshCount());
        dataView.removeSorting();
        Assert.assertEquals(2, ((ListDataViewImpl) dataView).getRefreshCount());
        dataView.addSortComparator(String::compareTo);
        Assert.assertEquals(3, ((ListDataViewImpl) dataView).getRefreshCount());
        dataView.setSortOrder(ValueProvider.identity(),
                SortDirection.ASCENDING);
        Assert.assertEquals(4, ((ListDataViewImpl) dataView).getRefreshCount());
        dataView.addSortOrder(ValueProvider.identity(),
                SortDirection.DESCENDING);
        Assert.assertEquals(5, ((ListDataViewImpl) dataView).getRefreshCount());
    }

    @Test
    public void setSortComparator_resetSortingWithDataView_dataProviderSortingNotAffected() {
        dataProvider.setSortComparator(String::compareTo);
        dataView.setSortComparator(null);
        Assert.assertArrayEquals(
                "Sorting reset in data view impacts the sorting in data provider",
                new String[] { "first", "last", "middle" },
                dataView.getItems().toArray());
    }

    @Test
    public void addSortComparator_twoComparatorsAdded_itemsSortedByCompositeComparator() {
        dataProvider = DataProvider.ofItems("b3", "a2", "a1");
        dataView = new ListDataViewImpl(() -> dataProvider, component);
        dataView.addSortComparator((s1, s2) -> Character.valueOf(s1.charAt(0))
                .compareTo(Character.valueOf(s2.charAt(0))));
        Assert.assertEquals("Unexpected data set order (comparator 1)",
                "a2,a1,b3",
                dataView.getItems().collect(Collectors.joining(",")));
        dataView.addSortComparator((s1, s2) -> Character.valueOf(s1.charAt(1))
                .compareTo(Character.valueOf(s2.charAt(1))));
        Assert.assertEquals("Unexpected data set order (comparator 2)",
                "a1,a2,b3",
                dataView.getItems().collect(Collectors.joining(",")));
    }

    @Test
    public void setSortOrder_sortOrderIsSet_sortedItemsObtained() {
        dataView.setSortOrder(ValueProvider.identity(),
                SortDirection.ASCENDING);
        Assert.assertEquals("Unexpected data set order", "first,last,middle",
                dataView.getItems().collect(Collectors.joining(",")));
    }

    @Test
    public void addSortOrder_twoOrdersAdded_itemsSortedByCompositeOrders() {
        dataProvider = DataProvider.ofItems("b3", "a1", "a2");
        dataView = new ListDataViewImpl(() -> dataProvider,
                new TestComponent());
        dataView.addSortOrder((item) -> item.charAt(0),
                SortDirection.DESCENDING);
        Assert.assertEquals("Unexpected data set order (order 1)", "b3,a1,a2",
                dataView.getItems().collect(Collectors.joining(",")));
        dataView.addSortOrder((item) -> item.charAt(1),
                SortDirection.DESCENDING);
        Assert.assertEquals("Unexpected data set order (order 2)", "b3,a2,a1",
                dataView.getItems().collect(Collectors.joining(",")));
    }

    @Test
    public void removeSorting_sortingSetAndThenRemoved_initialSortingObtained() {
        dataView.setSortOrder(ValueProvider.identity(),
                SortDirection.ASCENDING);
        Assert.assertEquals("Unexpected data set order", "first,last,middle",
                dataView.getItems().collect(Collectors.joining(",")));
        dataView.removeSorting();
        Assert.assertEquals("Unexpected data set order", "first,middle,last",
                dataView.getItems().collect(Collectors.joining(",")));
    }

    @Test
    public void contains_itemPresentedInDataSet_itemFound() {
        Assert.assertTrue("Set item was not found in the data",
                dataView.contains("first"));
    }

    @Test
    public void contains_itemNotPresentedInDataSet_itemNotFound() {
        Assert.assertFalse("Non existent item found in data",
                dataView.contains("absent item"));
    }

    @Test
    public void setIdentifierProvider_defaultIdentity_equalsIsUsed() {
        Assert.assertTrue(beanDataView.contains(new Item(1L, "value1")));
        Assert.assertFalse(beanDataView.contains(new Item(1L, "non present")));
        Assert.assertFalse(beanDataView.contains(new Item(4L, "value1")));
    }

    @Test
    public void setIdentifierProvider_dataProviderIdentity_getIdIsUsed() {
        itemListDataProvider = new AbstractDataViewTest.CustomIdentityItemDataProvider(
                getTestItems());

        Assert.assertTrue(beanDataView.contains(new Item(1L, "value1")));
        Assert.assertTrue(beanDataView.contains(new Item(1L, "non present")));
        Assert.assertFalse(beanDataView.contains(new Item(4L, "value1")));
    }

    @Test
    public void setIdentifierProvider_customIdentifierProvider_customIdentifierProviderIsUsed() {
        beanDataView.setIdentifierProvider(Item::getValue);

        Assert.assertTrue(beanDataView.contains(new Item(1L, "value1")));
        Assert.assertFalse(beanDataView.contains(new Item(1L, "non present")));
        Assert.assertTrue(beanDataView.contains(new Item(4L, "value1")));
    }

    @Test
    public void setIdentifierProvider_dataProviderHasChanged_newDataProviderIsUsed() {
        Assert.assertFalse(beanDataView.contains(new Item(1L, "non present")));

        itemListDataProvider = new AbstractDataViewTest.CustomIdentityItemDataProvider(
                getTestItems());

        Assert.assertTrue(beanDataView.contains(new Item(1L, "non present")));

        itemListDataProvider = DataProvider
                .ofItems(new Item(10L, "description10"));

        Assert.assertFalse(beanDataView.contains(new Item(1L, "non present")));
        Assert.assertTrue(
                beanDataView.contains(new Item(10L, "description10")));
    }

    @Test
    public void setIdentifierProvider_dataProviderHasChanged_identifierProviderRetained() {
        Assert.assertFalse(
                beanDataView.contains(new Item(4L, "non present", "descr1")));

        beanDataView.setIdentifierProvider(Item::getDescription);

        Assert.assertTrue(
                beanDataView.contains(new Item(4L, "non present", "descr1")));

        itemListDataProvider = new AbstractDataViewTest.CustomIdentityItemDataProvider(
                getTestItems());

        Assert.assertTrue(
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

    @Test(expected = NullPointerException.class)
    public void addIdentifierProviderChangeListener_doesNotAcceptNull() {
        beanDataView.addIdentifierProviderChangeListener(null);
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
        Assert.assertTrue(beanDataView.contains(new Item(1L, "value1")));

        beanDataView.setFilter(item -> item.getId() > 1L);

        Assert.assertFalse(beanDataView.contains(new Item(1L, "value1")));

        Assert.assertTrue(beanDataView.contains(new Item(3L, "value3")));
    }

    @Test
    public void addItem_itemInDataset() {
        final String newItem = "new Item";
        dataView.addItem(newItem);

        Assert.assertEquals(4, dataView.getItemCount());
        Assert.assertTrue(dataView.contains(newItem));
        Optional<String> optionalItem = dataView.getNextItem("last");
        Assert.assertTrue(optionalItem.isPresent());
        Assert.assertEquals(newItem, optionalItem.get());
    }

    @Test
    public void addItem_itemAlreadyInList_notAdded() {
        final String newItem = "first";
        dataView.addItem(newItem);

        Assert.assertEquals(3, dataView.getItemCount());
        Assert.assertTrue(dataView.contains(newItem));
    }

    @Test
    public void removeItem_itemRemovedFromDataset() {
        dataView.removeItem("middle");

        Assert.assertEquals(2, dataView.getItemCount());
        Assert.assertFalse(dataView.contains("middle"));
        Optional<String> optionalItem = dataView.getNextItem("first");
        Assert.assertTrue(optionalItem.isPresent());
        Assert.assertEquals("last", optionalItem.get());
    }

    @Test
    public void removeItem_notInList_dataSetNotChanged() {
        dataView.removeItem("not present");
        Assert.assertEquals(3, dataView.getItemCount());
    }

    @Test
    public void addItemBefore_itemIsAddedAtExpectedPosition() {
        dataView.addItemBefore("newItem", "middle");

        Assert.assertArrayEquals(
                new String[] { "first", "newItem", "middle", "last" },
                dataView.getItems().toArray(String[]::new));

        dataView.addItemBefore("second", "first");

        Assert.assertArrayEquals(
                new String[] { "second", "first", "newItem", "middle", "last" },
                dataView.getItems().toArray(String[]::new));

    }

    @Test
    public void addItemBefore_itemAlreadyInList_itemIsMovedAtExpectedPosition() {
        final String newItem = "newItem";
        dataView.addItem(newItem);

        dataView.addItemBefore("newItem", "middle");

        Assert.assertArrayEquals(
                new String[] { "first", "newItem", "middle", "last" },
                dataView.getItems().toArray(String[]::new));
    }

    @Test
    public void addItemBefore_itemAndTargetAreTheSame_itemIsNotAdded() {
        dataView.addItemBefore("newItem", "newItem");

        Assert.assertArrayEquals(new String[] { "first", "middle", "last" },
                dataView.getItems().toArray(String[]::new));
    }

    @Test
    public void addItemBefore_itemNotInCollection_throwsException() {
        exceptionRule.expect(IllegalArgumentException.class);
        exceptionRule.expectMessage(
                "Item to insert before is not available in the data");

        dataView.addItemBefore("newItem", "notExistent");
    }

    @Test
    public void addItemBefore_addItemInFilteredDataSet_itemAddedBeforeTheTarget() {
        dataView.addFilter(item -> !item.equalsIgnoreCase("first"));
        dataView.addItemBefore("newItem", "last");

        Assert.assertArrayEquals(new String[] { "middle", "newItem", "last" },
                dataView.getItems().toArray(String[]::new));
    }

    @Test
    public void addItemAfter_itemIsAddedAtExpectedPosition() {
        dataView.addItemAfter("newItem", "middle");

        Assert.assertArrayEquals(
                new String[] { "first", "middle", "newItem", "last" },
                dataView.getItems().toArray(String[]::new));

        dataView.addItemAfter("second", "last");

        Assert.assertArrayEquals(
                new String[] { "first", "middle", "newItem", "last", "second" },
                dataView.getItems().toArray(String[]::new));

        dataView.addItemAfter("middle", "last");

        Assert.assertArrayEquals(
                new String[] { "first", "newItem", "last", "middle", "second" },
                dataView.getItems().toArray(String[]::new));
    }

    @Test
    public void addItemAfter_itemAlreadyInList_itemIsMovedAtExpectedPosition() {
        final String newItem = "newItem";
        dataView.addItem(newItem);

        dataView.addItemAfter("newItem", "middle");

        Assert.assertArrayEquals(
                new String[] { "first", "middle", "newItem", "last" },
                dataView.getItems().toArray(String[]::new));
    }

    @Test
    public void addItemAfter_itemAndTargetAreTheSame_itemIsNotAdded() {
        dataView.addItemAfter("newItem", "newItem");

        Assert.assertArrayEquals(new String[] { "first", "middle", "last" },
                dataView.getItems().toArray(String[]::new));
    }

    @Test
    public void addItemAfter_itemNotInCollection_throwsException() {
        exceptionRule.expect(IllegalArgumentException.class);
        exceptionRule.expectMessage(
                "Item to insert after is not available in the data");

        dataView.addItemAfter("newItem", "notExistent");
    }

    @Test
    public void addItemAfter_addItemInFilteredDataSet_itemAddedAfterTheTarget() {
        dataView.addFilter(item -> !item.equalsIgnoreCase("first"));
        dataView.addItemAfter("newItem", "last");

        Assert.assertArrayEquals(new String[] { "middle", "last", "newItem" },
                dataView.getItems().toArray(String[]::new));
    }

    @Test
    public void addItems_allItemsAreAdded() {
        dataView.addItems(Arrays.asList("newOne", "newTwo", "newThree"));

        Assert.assertArrayEquals(
                new String[] { "first", "middle", "last", "newOne", "newTwo",
                        "newThree" },
                dataView.getItems().toArray(String[]::new));
    }

    @Test
    public void addItems_someItemsAlreadyInList_allItemsAreMovedAtTheEndAndOrdered() {
        dataView.addItems(Arrays.asList("first", "newOne", "newTwo"));

        Assert.assertArrayEquals(
                new String[] { "middle", "last", "first", "newOne", "newTwo" },
                dataView.getItems().toArray(String[]::new));
    }

    @Test
    public void addItems_nullCollectionPassed_throwsException() {
        exceptionRule.expect(NullPointerException.class);
        exceptionRule.expectMessage("Items collection cannot be null");

        dataView.addItems(null);
    }

    @Test
    public void addItems_emptyCollectionPassed_dataNotChanged() {
        dataView.addItems(Collections.emptyList());
        Assert.assertArrayEquals(new String[] { "first", "middle", "last" },
                dataView.getItems().toArray(String[]::new));
    }

    @Test
    public void addItemsAfter_allItemsAreAddedAfterTargetItem() {
        dataView.addItemsAfter(Arrays.asList("newOne", "newTwo", "newThree"),
                "first");

        Assert.assertArrayEquals(
                new String[] { "first", "newOne", "newTwo", "newThree",
                        "middle", "last" },
                dataView.getItems().toArray(String[]::new));
    }

    @Test
    public void addItemsAfter_someItemsAlreadyInList_allItemsAreMovedAfterTargetAndOrdered() {
        dataView.addItemsAfter(Arrays.asList("middle", "newOne", "newTwo"),
                "first");

        Assert.assertArrayEquals(
                new String[] { "first", "middle", "newOne", "newTwo", "last" },
                dataView.getItems().toArray(String[]::new));

        dataView.addItemsAfter(Collections.singletonList("newThree"), "last");

        Assert.assertArrayEquals(
                new String[] { "first", "middle", "newOne", "newTwo", "last",
                        "newThree" },
                dataView.getItems().toArray(String[]::new));

        dataView.addItemsAfter(Arrays.asList("newFour", "newThree"),
                "newThree");

        Assert.assertArrayEquals(
                new String[] { "first", "middle", "newOne", "newTwo", "last",
                        "newFour", "newThree" },
                dataView.getItems().toArray(String[]::new));

        dataView.addItemsAfter(Arrays.asList("newFive", "first"), "first");

        Assert.assertArrayEquals(
                new String[] { "newFive", "first", "middle", "newOne", "newTwo",
                        "last", "newFour", "newThree" },
                dataView.getItems().toArray(String[]::new));
    }

    @Test
    public void addItemsAfter_targetItemNotInCollection_throwsException() {
        exceptionRule.expect(IllegalArgumentException.class);
        exceptionRule.expectMessage(
                "Item to insert after is not available in the data");

        dataView.addItemsAfter(Collections.singletonList("newItem"),
                "notExistent");
    }

    @Test
    public void addItemsAfter_nullCollectionPassed_throwsException() {
        exceptionRule.expect(NullPointerException.class);
        exceptionRule.expectMessage("Items collection cannot be null");

        dataView.addItemsAfter(null, "any");
    }

    @Test
    public void addItemsAfter_emptyCollectionPassed_dataNotChanged() {
        dataView.addItemsAfter(Collections.emptyList(), "any");
        Assert.assertArrayEquals(new String[] { "first", "middle", "last" },
                dataView.getItems().toArray(String[]::new));
    }

    @Test
    public void addItemsAfter_addItemsInFilteredDataSet_itemsAddedAfterTheTarget() {
        dataView.addFilter(item -> !item.equalsIgnoreCase("first"));
        dataView.addItemsAfter(Arrays.asList("newItem1", "newItem2"), "last");

        Assert.assertArrayEquals(
                new String[] { "middle", "last", "newItem1", "newItem2" },
                dataView.getItems().toArray(String[]::new));
    }

    @Test
    public void addItemsBefore_allItemsAreAddedBeforeTargetItem() {
        dataView.addItemsBefore(Arrays.asList("newOne", "newTwo", "newThree"),
                "middle");

        Assert.assertArrayEquals(
                new String[] { "first", "newOne", "newTwo", "newThree",
                        "middle", "last" },
                dataView.getItems().toArray(String[]::new));
    }

    @Test
    public void addItemsBefore_someItemsAlreadyInList_allItemsAreMovedBeforeTargetAndOrdered() {

        dataView.addItemsBefore(Arrays.asList("first", "newOne", "newTwo"),
                "last");

        Assert.assertArrayEquals(
                new String[] { "middle", "first", "newOne", "newTwo", "last" },
                dataView.getItems().toArray(String[]::new));

        dataView.addItemsBefore(Arrays.asList("newThree", "last"), "last");

        Assert.assertArrayEquals(
                new String[] { "middle", "first", "newOne", "newTwo",
                        "newThree", "last" },
                dataView.getItems().toArray(String[]::new));

        dataView.addItemsBefore(Arrays.asList("newFour", "middle"), "middle");

        Assert.assertArrayEquals(
                new String[] { "newFour", "middle", "first", "newOne", "newTwo",
                        "newThree", "last" },
                dataView.getItems().toArray(String[]::new));

        dataView.addItemsBefore(Collections.singletonList("newFive"),
                "newFour");

        Assert.assertArrayEquals(
                new String[] { "newFive", "newFour", "middle", "first",
                        "newOne", "newTwo", "newThree", "last" },
                dataView.getItems().toArray(String[]::new));
    }

    @Test
    public void addItemsBefore_targetItemNotInCollection_throwsException() {
        exceptionRule.expect(IllegalArgumentException.class);
        exceptionRule.expectMessage(
                "Item to insert before is not available in the data");

        dataView.addItemsBefore(Collections.singletonList("newItem"),
                "notExistent");
    }

    @Test
    public void addItemsBefore_nullCollectionPassed_throwsException() {
        exceptionRule.expect(NullPointerException.class);
        exceptionRule.expectMessage("Items collection cannot be null");

        dataView.addItemsBefore(null, "any");
    }

    @Test
    public void addItemsBefore_emptyCollectionPassed_dataNotChanged() {
        dataView.addItemsBefore(Collections.emptyList(), "any");
        Assert.assertArrayEquals(new String[] { "first", "middle", "last" },
                dataView.getItems().toArray(String[]::new));
    }

    @Test
    public void addItemsBefore_addItemsInFilteredDataSet_itemsAddedBeforeTheTarget() {
        dataView.addFilter(item -> !item.equalsIgnoreCase("first"));
        dataView.addItemsBefore(Arrays.asList("newItem1", "newItem2"), "last");

        Assert.assertArrayEquals(
                new String[] { "middle", "newItem1", "newItem2", "last" },
                dataView.getItems().toArray(String[]::new));
    }

    @Test
    public void removeItems_itemsOutOfOrder_allItemsAreRemoved() {
        dataView.removeItems(Arrays.asList("middle", "first"));

        Assert.assertArrayEquals(new String[] { "last" },
                dataView.getItems().toArray(String[]::new));
    }

    @Test
    public void removeItems_nullCollectionPassed_throwsException() {
        exceptionRule.expect(NullPointerException.class);
        exceptionRule.expectMessage("Items collection cannot be null");

        dataView.removeItems(null);
    }

    @Test
    public void removeItems_emptyCollectionPassed_dataNotChanged() {
        dataView.removeItems(Collections.emptyList());
        Assert.assertArrayEquals(new String[] { "first", "middle", "last" },
                dataView.getItems().toArray(String[]::new));
    }

    @Test
    public void setItems_nullCollectionPassed_throwsException() {
        exceptionRule.expect(NullPointerException.class);
        exceptionRule.expectMessage("Items collection cannot be null");

        dataView.setItems(null);
    }

    @Test
    public void setItems_emptyCollectionPassed_dataEmpty() {
        dataView.setItems(Collections.emptyList());
        Assert.assertTrue(dataView.getItems().toList().isEmpty());
    }

    @Test
    public void setItems_collectionPassed_dataFilled() {
        dataView.setItems(List.of("first", "middle", "last"));
        Assert.assertArrayEquals(new String[] { "first", "middle", "last" },
                dataView.getItems().toArray(String[]::new));
    }

    @Test
    public void addItemsAndRemoveItems_noConcurrencyIssues() {
        dataView.addItemsBefore(Arrays.asList("newOne", "newTwo", "newThree"),
                "middle");

        Assert.assertArrayEquals(
                new String[] { "first", "newOne", "newTwo", "newThree",
                        "middle", "last" },
                dataView.getItems().toArray(String[]::new));

        dataView.removeItems(Arrays.asList("middle", "first"));

        Assert.assertArrayEquals(
                new String[] { "newOne", "newTwo", "newThree", "last" },
                dataView.getItems().toArray(String[]::new));

        dataView.addItemsAfter(Arrays.asList("one", "two"), "newOne");

        Assert.assertArrayEquals(new String[] { "newOne", "one", "two",
                "newTwo", "newThree", "last" },
                dataView.getItems().toArray(String[]::new));

    }

    @Test
    public void dataProviderOnSet_exceptionThrownForAddItemBefore() {
        exceptionRule.expect(IllegalArgumentException.class);
        exceptionRule.expectMessage(
                "DataProvider collection 'HashSet' is not a list.");

        Set<String> items = new HashSet<>();
        items.add("item1");
        items.add("item2");

        final ListDataProvider<String> stringListDataProvider = new ListDataProvider<>(
                items);
        dataView = new ListDataViewImpl(() -> stringListDataProvider,
                component);

        dataView.addItemBefore("newItem", "item2");
    }

    @Test
    public void dataProviderOnSet_exceptionThrownForAddItemAfter() {
        exceptionRule.expect(IllegalArgumentException.class);
        exceptionRule.expectMessage(
                "DataProvider collection 'HashSet' is not a list.");

        Set<String> items = new HashSet<>();
        items.add("item1");
        items.add("item2");

        final ListDataProvider<String> stringListDataProvider = new ListDataProvider<>(
                items);
        dataView = new ListDataViewImpl(() -> stringListDataProvider, null);

        dataView.addItemAfter("newItem", "item1");
    }

    @Test
    public void dataProviderOnSet_exceptionThrownForAddItemsAfter() {
        exceptionRule.expect(IllegalArgumentException.class);
        exceptionRule.expectMessage(
                "DataProvider collection 'HashSet' is not a list.");

        Set<String> items = new HashSet<>();
        items.add("item1");
        items.add("item2");

        final ListDataProvider<String> stringListDataProvider = new ListDataProvider<>(
                items);
        dataView = new ListDataViewImpl(() -> stringListDataProvider,
                component);

        dataView.addItemsAfter(Collections.singleton("newItem"), "item1");
    }

    @Test
    public void dataProviderOnSet_exceptionThrownForAddItemsBefore() {
        exceptionRule.expect(IllegalArgumentException.class);
        exceptionRule.expectMessage(
                "DataProvider collection 'HashSet' is not a list.");

        Set<String> items = new HashSet<>();
        items.add("item1");
        items.add("item2");

        final ListDataProvider<String> stringListDataProvider = new ListDataProvider<>(
                items);
        dataView = new ListDataViewImpl(() -> stringListDataProvider,
                component);

        dataView.addItemsBefore(Collections.singleton("newItem"), "item1");
    }

    @Test
    public void addFilter_FilterIsAddedOnTop() {
        items = new ArrayList<>(
                Arrays.asList("item1", "item2", "item22", "item3"));
        dataProvider = DataProvider.ofCollection(items);

        Assert.assertEquals(4, dataView.getItems().count());

        dataView.addFilter(item -> item.equals("item1") || item.equals("item2")
                || item.equals("item22"));

        Assert.assertEquals(3, dataView.getItems().count());

        dataView.addFilter(item -> item.endsWith("2"));

        Assert.assertEquals(2, dataView.getItems().count());
    }

    @Test
    public void removeFilters_removesAllSetAndAddedFilters() {
        items = new ArrayList<>(
                Arrays.asList("item1", "item2", "item22", "item3"));
        dataProvider = DataProvider.ofCollection(items);

        dataView.setFilter(item -> item.endsWith("2") || item.endsWith("3"));

        Assert.assertEquals("Set filter not applied", 3,
                dataView.getItems().count());

        dataView.addFilter(item -> item.endsWith("2"));

        Assert.assertEquals("Added filter not applied", 2,
                dataView.getItems().count());

        dataView.removeFilters();

        Assert.assertEquals("Filters were not cleared", 4,
                dataView.getItems().count());
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
        Assert.assertEquals("Wrong item returned for index", "first",
                dataView.getItem(0));
    }

    @Test
    public void getItem_negativeIndex_throwsException() {
        exceptionRule.expect(IndexOutOfBoundsException.class);
        exceptionRule.expectMessage(
                "Given index -1 is outside of the accepted range '0 - 2'");
        dataView.getItem(-1);
    }

    @Test
    public void getItem_emptyDataSet_throwsException() {
        dataProvider = DataProvider.ofItems();
        exceptionRule.expect(IndexOutOfBoundsException.class);
        exceptionRule.expectMessage("Requested index 0 on empty data.");
        dataView.getItem(0);
    }

    @Test
    public void getItem_filteringApplied_itemFound() {
        Assert.assertEquals("Wrong item returned for index", "middle",
                dataView.getItem(1));
        dataProvider
                .setFilter(item -> "first".equals(item) || "last".equals(item));
        Assert.assertEquals("Wrong item returned for index", "last",
                dataView.getItem(1));
    }

    @Test
    public void getItem_sortingApplied_itemFound() {
        Assert.assertEquals("Wrong item returned for index", "first",
                dataView.getItem(0));
        dataProvider.setSortOrder(item -> item, SortDirection.DESCENDING);
        Assert.assertEquals("Wrong item returned for index", "middle",
                dataView.getItem(0));
    }

    @Test
    public void getItem_indexOutsideOfSize_throwsException() {
        exceptionRule.expect(IndexOutOfBoundsException.class);
        dataView.getItem(items.size());
    }

    @Test
    public void getItemIndex_itemPresentedInDataSet_indexFound() {
        Assert.assertEquals("Wrong index returned for item", Optional.of(1),
                dataView.getItemIndex("middle"));
    }

    @Test
    public void getItemIndex_itemNotPresentedInDataSet_indexNotFound() {
        Assert.assertEquals("Wrong index returned for item", Optional.empty(),
                dataView.getItemIndex("notPresent"));
    }

    @Test
    public void getItemIndex_filteringApplied_indexFound() {
        dataProvider
                .setFilter(item -> "first".equals(item) || "last".equals(item));
        Assert.assertEquals("Wrong index returned for item", Optional.of(1),
                dataView.getItemIndex("last"));
    }

    @Test
    public void getItemIndex_sortingApplied_indexFound() {
        dataProvider.setSortOrder(item -> item, SortDirection.DESCENDING);
        Assert.assertEquals("Wrong index returned for item", Optional.of(0),
                dataView.getItemIndex("middle"));
    }

    @Test
    public void getItemIndex_itemNotPresentedInDataSet_filteringApplied_indexNotFound() {
        dataProvider
                .setFilter(item -> "first".equals(item) || "last".equals(item));
        Assert.assertEquals("Wrong index returned for item", Optional.empty(),
                dataView.getItemIndex("middle"));
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
        Assert.assertEquals(itemCount + 1, listDataView.getItemCount());
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
        Assert.assertEquals(itemCount - 1, listDataView.getItemCount());
    }

    @Test
    public void setFilter_twoComponentsHasSameDataProvider_onlyTargetComponentImpacted() {
        TestComponent component1 = new TestComponent();
        TestComponent component2 = new TestComponent();

        ListDataViewImpl listDataView1 = new ListDataViewImpl(
                () -> dataProvider, component1);

        ListDataViewImpl listDataView2 = new ListDataViewImpl(
                () -> dataProvider, component2);

        Assert.assertEquals("Unexpected initial items count for component #1",
                3, listDataView1.getItemCount());

        Assert.assertEquals("Unexpected initial items count for component #2",
                3, listDataView2.getItemCount());

        listDataView1.setFilter(
                item -> "middle".equals(item) || "last".equals(item));

        Assert.assertNull("Unexpected delegation of filtering to data provider",
                dataProvider.getFilter());

        Assert.assertEquals(
                "Unexpected component #1 items count after filter apply", 2,
                listDataView1.getItemCount());

        Assert.assertEquals(
                "Unexpected component #2 items count after filter apply to component #1",
                3, listDataView2.getItemCount());

        Assert.assertArrayEquals("Unexpected items after filter apply",
                new String[] { "middle", "last" },
                listDataView1.getItems().toArray());

        Assert.assertArrayEquals("Unexpected items after filter apply",
                new String[] { "first", "middle", "last" },
                listDataView2.getItems().toArray());

        listDataView1.addFilter("middle"::equals);

        Assert.assertEquals(
                "Unexpected component #1 items count after filter apply", 1,
                listDataView1.getItemCount());

        Assert.assertEquals(
                "Unexpected component #2 items count after filter apply to component #1",
                3, listDataView2.getItemCount());

        Assert.assertArrayEquals("Unexpected items after filter apply",
                new String[] { "middle" }, listDataView1.getItems().toArray());

        Assert.assertArrayEquals("Unexpected items after filter apply",
                new String[] { "first", "middle", "last" },
                listDataView2.getItems().toArray());

        listDataView1.removeFilters();

        Assert.assertEquals(
                "Unexpected component #1 items count after filter remove", 3,
                listDataView1.getItemCount());

        Assert.assertEquals(
                "Unexpected component #2 items count after filter remove in component #1",
                3, listDataView2.getItemCount());

        Assert.assertArrayEquals("Unexpected items after filter remove",
                new String[] { "first", "middle", "last" },
                listDataView1.getItems().toArray());

        Assert.assertArrayEquals("Unexpected items after filter remove",
                new String[] { "first", "middle", "last" },
                listDataView2.getItems().toArray());
    }

    @Test
    public void setFilter_setDataProviderFilter_bothDataViewAndDataProviderFilterAreApplied() {
        TestComponent component = new TestComponent();

        ListDataViewImpl listDataView = new ListDataViewImpl(() -> dataProvider,
                component);

        listDataView.setFilter(
                item -> "middle".equals(item) || "last".equals(item));

        dataProvider.setFilter("middle"::equals);

        Assert.assertArrayEquals(
                "Unexpected items after applying filter to both component"
                        + " and data provider",
                new String[] { "middle" }, listDataView.getItems().toArray());

        dataProvider.clearFilters();

        Assert.assertArrayEquals(
                "Unexpected items after clearing data " + "provider's filter",
                new String[] { "middle", "last" },
                listDataView.getItems().toArray());
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

        Assert.assertNull("Unexpected delegation of sorting to data provider",
                dataProvider.getSortComparator());

        Assert.assertArrayEquals("Unexpected items sorting for component #1",
                new Long[] { 1L, 1L, 2L },
                listDataView1.getItems().map(Item::getId).toArray());

        Assert.assertArrayEquals("Unexpected items sorting for component #2",
                new Long[] { 1L, 2L, 1L },
                listDataView2.getItems().map(Item::getId).toArray());

        listDataView1.addSortComparator((item1, item2) -> item1.getValue()
                .compareToIgnoreCase(item2.getValue()));

        Assert.assertArrayEquals("Unexpected items sorting for component #1",
                new String[] { "bar", "baz", "foo" },
                listDataView1.getItems().map(Item::getValue).toArray());

        Assert.assertArrayEquals("Unexpected items sorting for component #2",
                new String[] { "baz", "foo", "bar" },
                listDataView2.getItems().map(Item::getValue).toArray());

        listDataView1.removeSorting();

        Assert.assertArrayEquals("Unexpected items sorting for component #1",
                new Item[] { new Item(1L, "baz"), new Item(2L, "foo"),
                        new Item(1L, "bar") },
                listDataView1.getItems().toArray());

        Assert.assertArrayEquals("Unexpected items sorting for component #2",
                new Item[] { new Item(1L, "baz"), new Item(2L, "foo"),
                        new Item(1L, "bar") },
                listDataView2.getItems().toArray());
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

        Assert.assertArrayEquals("Unexpected items sorting for component #1",
                new Long[] { 1L, 1L, 2L },
                listDataView1.getItems().map(Item::getId).toArray());

        Assert.assertArrayEquals("Unexpected items sorting for component #2",
                new Long[] { 1L, 2L, 1L },
                listDataView2.getItems().map(Item::getId).toArray());

        listDataView1.addSortOrder(Item::getValue, SortDirection.ASCENDING);

        Assert.assertArrayEquals("Unexpected items sorting for component #1",
                new String[] { "bar", "baz", "foo" },
                listDataView1.getItems().map(Item::getValue).toArray());

        Assert.assertArrayEquals("Unexpected items sorting for component #2",
                new String[] { "baz", "foo", "bar" },
                listDataView2.getItems().map(Item::getValue).toArray());

        listDataView1.removeSorting();

        Assert.assertArrayEquals("Unexpected items sorting for component #1",
                new Item[] { new Item(1L, "baz"), new Item(2L, "foo"),
                        new Item(1L, "bar") },
                listDataView1.getItems().toArray());

        Assert.assertArrayEquals("Unexpected items sorting for component #2",
                new Item[] { new Item(1L, "baz"), new Item(2L, "foo"),
                        new Item(1L, "bar") },
                listDataView2.getItems().toArray());
    }

    @Test
    public void setSortComparator_setDataProviderSorting_bothDataViewAndDataProviderSortingAreApplied() {
        TestComponent component = new TestComponent();

        ListDataProvider<Item> dataProvider = DataProvider.ofItems(
                new Item(2L, "bar"), new Item(3L, "foo"), new Item(1L, "bar"));

        ItemListDataView listDataView = new ItemListDataView(() -> dataProvider,
                component);

        listDataView.setSortOrder(Item::getValue, SortDirection.ASCENDING);

        Assert.assertArrayEquals(
                "Unexpected items after applying filter to both component"
                        + " and data provider",
                new Item[] { new Item(2L, "bar"), new Item(1L, "bar"),
                        new Item(3L, "foo") },
                listDataView.getItems().toArray());

        dataProvider.setSortOrder(Item::getId, SortDirection.ASCENDING);

        Assert.assertArrayEquals(
                "Unexpected items after applying filter to both component"
                        + " and data provider",
                new Item[] { new Item(1L, "bar"), new Item(2L, "bar"),
                        new Item(3L, "foo") },
                listDataView.getItems().toArray());

        dataProvider.setSortComparator(null);

        Assert.assertArrayEquals(
                "Unexpected items after applying filter to both component"
                        + " and data provider",
                new Item[] { new Item(2L, "bar"), new Item(1L, "bar"),
                        new Item(3L, "foo") },
                listDataView.getItems().toArray());
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

        Assert.assertNotNull(filtering.get());
        Assert.assertArrayEquals(new String[] { "bar" },
                items.stream().filter(filtering.get()).toArray());
        Assert.assertNull(sorting.get());

        listDataView.removeFilters();
        listDataView.setSortOrder(String::toLowerCase, SortDirection.ASCENDING);
        Assert.assertNotNull(sorting.get());
        Assert.assertNull(filtering.get());
        Assert.assertArrayEquals(new String[] { "bar", "baz", "foo" },
                items.stream().sorted(sorting.get()).toArray());

        listDataView.setSortComparator(null);
        Assert.assertNull(sorting.get());
    }

    @Test
    public void filterOrSortingChangedCallback_emptyCallbackProvided_throws() {
        exceptionRule.expect(NullPointerException.class);
        exceptionRule.expectMessage(
                "Filter or Sorting Change Callback cannot be empty");
        new ListDataViewImpl(() -> dataProvider, component, null);
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

    private Collection<Item> getTestItems() {
        return new ArrayList<>(Arrays.asList(new Item(1L, "value1", "descr1"),
                new Item(2L, "value2", "descr2"),
                new Item(3L, "value3", "descr3")));
    }

    @Tag("test-component")
    private static class TestComponent extends Component {
    }
}
