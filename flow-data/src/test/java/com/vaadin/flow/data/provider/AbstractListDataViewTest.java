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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.google.common.primitives.Chars;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.function.ValueProvider;
import com.vaadin.flow.tests.data.bean.Item;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.function.SerializableSupplier;
import org.mockito.Mockito;

public class AbstractListDataViewTest {

    private Collection<String> items;

    @Rule
    public ExpectedException exceptionRule = ExpectedException.none();

    private ListDataProvider<String> dataProvider;

    private AbstractListDataView<String> dataView;

    private Component component;

    @Before
    public void init() {
        items = new ArrayList<>(Arrays.asList("first", "middle", "last"));
        dataProvider = DataProvider.ofCollection(items);
        component = new TestComponent();
        dataView = new ListDataViewImpl(() -> dataProvider, component);
    }

    @Test
    public void createListDataViewInstance_faultyDataProvider_throwsException() {
        DataProvider dataProvider = DataProvider
                .fromCallbacks(query -> Stream.of("one"), query -> 1);
        exceptionRule.expect(IllegalStateException.class);
        exceptionRule.expectMessage(
                "ListDataViewImpl only supports 'ListDataProvider' " +
                        "or it's subclasses, but was given a " +
                        "'AbstractBackEndDataProvider'");
        new ListDataViewImpl(() -> dataProvider, null);
    }

    @Test
    public void getNextItem_nextItemAvailable_nextItemFound() {
        Optional<String> middle = dataView.getNextItem("middle");
        Assert.assertTrue(middle.isPresent());
        Assert.assertEquals("Faulty next item", "last",
                middle.get());
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
        Assert.assertEquals("Item in middle should have previous item",
                "first", middle.get());
    }

    @Test
    public void getPrevItem_prevItemUnavailable_prevItemNotFound() {
        Assert.assertFalse("Got previous item for first index",
                dataView.getPreviousItem("first").isPresent());
    }

    @Test
    public void setFilter_filterIsSet_filteredItemsObtained() {
        Assert.assertEquals(items.size(), dataView.getSize());
        dataView.setFilter(item -> item.equals("first"));
        Assert.assertEquals("Filter was not applied to data size", 1,
                dataView.getSize());
        Assert.assertEquals("Expected item is missing from filtered data",
                "first", dataView.getItems().findFirst().get());
    }

    @Test
    public void setFilter_filterReset_allItemsObtained() {
        dataProvider.setFilter(item -> item.equals("first"));
        dataView.setFilter(null);
        Assert.assertEquals("Filter reset was not applied to data size",
                items.size(), dataView.getSize());
        Assert.assertArrayEquals("Filter reset was not applied to data set",
                items.toArray(), dataView.getItems().toArray());
    }

    @Test
    public void setSortComparator_sortIsSet_sortedItemsObtained() {
        dataView.setSortComparator(String::compareTo);
        Assert.assertEquals("Unexpected data set order after comparator setup",
                "first,last,middle",
                dataView.getItems().collect(Collectors.joining(",")));
    }

    @Test
    public void setSortComparator_sortingReset_sortingResetToInitial() {
        dataProvider.setSortComparator(String::compareTo);
        dataView.setSortComparator(null);
        Assert.assertArrayEquals("Sorting reset was not applied to data set",
                items.toArray(), dataView.getItems().toArray());
    }

    @Test
    public void addSortComparator_twoComparatorsAdded_itemsSortedByCompositeComparator() {
        dataProvider = DataProvider.ofItems("b3", "a2", "a1");
        dataView = new ListDataViewImpl(() -> dataProvider, component);
        dataView.addSortComparator((s1, s2) ->
                Chars.compare(s1.charAt(0), s2.charAt(0)));
        Assert.assertEquals("Unexpected data set order (comparator 1)",
                "a2,a1,b3",
                dataView.getItems().collect(Collectors.joining(",")));
        dataView.addSortComparator((s1, s2) ->
                Chars.compare(s1.charAt(1), s2.charAt(1)));
        Assert.assertEquals("Unexpected data set order (comparator 2)",
                "a1,a2,b3",
                dataView.getItems().collect(Collectors.joining(",")));
    }

    @Test
    public void setSortOrder_sortOrderIsSet_sortedItemsObtained() {
        dataView.setSortOrder(ValueProvider.identity(), SortDirection.ASCENDING);
        Assert.assertEquals("Unexpected data set order", "first,last,middle",
                dataView.getItems().collect(Collectors.joining(",")));
    }

    @Test
    public void addSortOrder_twoOrdersAdded_itemsSortedByCompositeOrders() {
        dataProvider = DataProvider.ofItems("b3", "a1", "a2");
        dataView = new ListDataViewImpl(() -> dataProvider, null);
        dataView.addSortOrder((item) -> item.charAt(0), SortDirection.DESCENDING);
        Assert.assertEquals("Unexpected data set order (order 1)",
                "b3,a1,a2",
                dataView.getItems().collect(Collectors.joining(",")));
        dataView.addSortOrder((item) -> item.charAt(1), SortDirection.DESCENDING);
        Assert.assertEquals("Unexpected data set order (order 2)",
                "b3,a2,a1",
                dataView.getItems().collect(Collectors.joining(",")));
    }

    @Test
    public void removeSorting_sortingSetAndThenRemoved_initialSortingObtained() {
        dataView.setSortOrder(ValueProvider.identity(), SortDirection.ASCENDING);
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
    public void addItem_itemInDataset() {
        final String newItem = "new Item";
        dataView.addItem(newItem);

        Assert.assertEquals(4, dataView.getSize());
        Assert.assertTrue(dataView.contains(newItem));
        Optional<String> optionalItem = dataView.getNextItem("last");
        Assert.assertTrue(optionalItem.isPresent());
        Assert.assertEquals(newItem, optionalItem.get());
    }

    @Test
    public void addItem_itemAlreadyInList_notAdded() {
        final String newItem = "first";
        dataView.addItem(newItem);

        Assert.assertEquals(3, dataView.getSize());
        Assert.assertTrue(dataView.contains(newItem));
    }

    @Test
    public void removeItem_itemRemovedFromDataset() {
        dataView.removeItem("middle");

        Assert.assertEquals(2, dataView.getSize());
        Assert.assertFalse(dataView.contains("middle"));
        Optional<String> optionalItem = dataView.getNextItem("first");
        Assert.assertTrue(optionalItem.isPresent());
        Assert.assertEquals("last", optionalItem.get());
    }

    @Test
    public void removeItem_notInList_dataSetNotChanged() {
        dataView.removeItem("not present");
        Assert.assertEquals(3, dataView.getSize());
    }

    @Test
    public void dataViewWithItem_rowOutsideSetRequested_exceptionThrown() {
        exceptionRule.expect(IndexOutOfBoundsException.class);
        exceptionRule.expectMessage(
                "Given index 7 is outside of the accepted range '0 - 2'");

        dataView.validateItemIndex(7);
    }

    @Test
    public void dataViewWithItem_negativeRowRequested_exceptionThrown() {
        exceptionRule.expect(IndexOutOfBoundsException.class);
        exceptionRule.expectMessage(
                "Given index -7 is outside of the accepted range '0 - 2'");

        dataView.validateItemIndex(-7);
    }

    @Test
    public void dataViewWithoutItems_exceptionThrown() {
        exceptionRule.expect(IndexOutOfBoundsException.class);
        exceptionRule.expectMessage("Requested index 5 on empty data.");

        dataProvider = DataProvider.ofCollection(Collections.emptyList());
        dataView = new ListDataViewImpl(() -> dataProvider, component);
        dataView.validateItemIndex(5);
    }

    @Test
    public void addItemBefore_itemIsAddedAtExpectedPosition() {
        dataView.addItemBefore("newItem", "middle");

        Assert.assertArrayEquals(
                new String[]{"first", "newItem", "middle", "last"},
                dataView.getItems().toArray(String[]::new));

        dataView.addItemBefore("second", "first");

        Assert.assertArrayEquals(
                new String[]{"second", "first", "newItem", "middle", "last"},
                dataView.getItems().toArray(String[]::new));

    }

    @Test
    public void addItemBefore_itemAlreadyInList_itemIsMovedAtExpectedPosition() {
        final String newItem = "newItem";
        dataView.addItem(newItem);

        dataView.addItemBefore("newItem", "middle");

        Assert.assertArrayEquals(
                new String[]{"first", "newItem", "middle", "last"},
                dataView.getItems().toArray(String[]::new));
    }

    @Test
    public void addItemBefore_itemAndTargetAreTheSame_itemIsNotAdded() {
        dataView.addItemBefore("newItem", "newItem");

        Assert.assertArrayEquals(
                new String[]{"first", "middle", "last"},
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
    public void addItemAfter_itemIsAddedAtExpectedPosition() {
        dataView.addItemAfter("newItem", "middle");

        Assert.assertArrayEquals(
                new String[]{"first", "middle", "newItem", "last"},
                dataView.getItems().toArray(String[]::new));

        dataView.addItemAfter("second", "last");

        Assert.assertArrayEquals(
                new String[]{"first", "middle", "newItem", "last", "second"},
                dataView.getItems().toArray(String[]::new));

        dataView.addItemAfter("middle", "last");

        Assert.assertArrayEquals(
                new String[]{"first", "newItem", "last", "middle", "second"},
                dataView.getItems().toArray(String[]::new));
    }

    @Test
    public void addItemAfter_itemAlreadyInList_itemIsMovedAtExpectedPosition() {
        final String newItem = "newItem";
        dataView.addItem(newItem);

        dataView.addItemAfter("newItem", "middle");

        Assert.assertArrayEquals(
                new String[]{"first", "middle", "newItem", "last"},
                dataView.getItems().toArray(String[]::new));
    }

    @Test
    public void addItemAfter_itemAndTargetAreTheSame_itemIsNotAdded() {
        dataView.addItemAfter("newItem", "newItem");

        Assert.assertArrayEquals(
                new String[]{"first", "middle", "last"},
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
    public void addItems_allItemsAreAdded() {
        dataView.addItems(Arrays.asList("newOne", "newTwo", "newThree"));

        Assert.assertArrayEquals(
                new String[]{"first", "middle", "last", "newOne", "newTwo",
                        "newThree"},
                dataView.getItems().toArray(String[]::new));
    }

    @Test
    public void addItems_someItemsAlreadyInList_allItemsAreMovedAtTheEndAndOrdered() {
        dataView.addItems(Arrays.asList("first", "newOne", "newTwo"));

        Assert.assertArrayEquals(
                new String[]{"middle", "last", "first", "newOne", "newTwo"},
                dataView.getItems().toArray(String[]::new));
    }

    @Test
    public void addItems_nullCollectionPassed_throwsException() {
        exceptionRule.expect(NullPointerException.class);
        exceptionRule.expectMessage(
                "Items collection cannot be null");

        dataView.addItems(null);
    }

    @Test
    public void addItems_emptyCollectionPassed_dataNotChanged() {
        dataView.addItems(Collections.emptyList());
        Assert.assertArrayEquals(
                new String[]{"first", "middle", "last"},
                dataView.getItems().toArray(String[]::new));
    }

    @Test
    public void addItemsAfter_allItemsAreAddedAfterTargetItem() {
        dataView.addItemsAfter(Arrays.asList("newOne", "newTwo", "newThree"),
                "first");

        Assert.assertArrayEquals(
                new String[]{"first", "newOne", "newTwo", "newThree",
                        "middle", "last"},
                dataView.getItems().toArray(String[]::new));
    }

    @Test
    public void addItemsAfter_someItemsAlreadyInList_allItemsAreMovedAfterTargetAndOrdered() {
        dataView.addItemsAfter(Arrays.asList("middle", "newOne", "newTwo"),
                "first");

        Assert.assertArrayEquals(
                new String[]{"first", "middle", "newOne", "newTwo", "last"},
                dataView.getItems().toArray(String[]::new));

        dataView.addItemsAfter(Collections.singletonList("newThree"),
                "last");

        Assert.assertArrayEquals(
                new String[]{"first", "middle", "newOne", "newTwo", "last", "newThree"},
                dataView.getItems().toArray(String[]::new));

        dataView.addItemsAfter(Arrays.asList("newFour", "newThree"),
                "newThree");

        Assert.assertArrayEquals(
                new String[]{"first", "middle", "newOne", "newTwo", "last", "newFour", "newThree"},
                dataView.getItems().toArray(String[]::new));

        dataView.addItemsAfter(Arrays.asList("newFive", "first"),
                "first");

        Assert.assertArrayEquals(
                new String[]{"newFive", "first", "middle", "newOne", "newTwo", "last", "newFour", "newThree"},
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
        exceptionRule.expectMessage(
                "Items collection cannot be null");

        dataView.addItemsAfter(null, "any");
    }

    @Test
    public void addItemsAfter_emptyCollectionPassed_dataNotChanged() {
        dataView.addItemsAfter(Collections.emptyList(), "any");
        Assert.assertArrayEquals(
                new String[]{"first", "middle", "last"},
                dataView.getItems().toArray(String[]::new));
    }

    @Test
    public void addItemsBefore_allItemsAreAddedBeforeTargetItem() {
        dataView.addItemsBefore(Arrays.asList("newOne", "newTwo", "newThree"),
                "middle");

        Assert.assertArrayEquals(
                new String[]{"first", "newOne", "newTwo", "newThree",
                        "middle", "last"},
                dataView.getItems().toArray(String[]::new));
    }

    @Test
    public void addItemsBefore_someItemsAlreadyInList_allItemsAreMovedBeforeTargetAndOrdered() {

        dataView.addItemsBefore(Arrays.asList("first", "newOne", "newTwo"),
                "last");

        Assert.assertArrayEquals(
                new String[]{"middle", "first", "newOne", "newTwo", "last"},
                dataView.getItems().toArray(String[]::new));

        dataView.addItemsBefore(Arrays.asList("newThree", "last"),
                "last");

        Assert.assertArrayEquals(
                new String[]{"middle", "first", "newOne", "newTwo", "newThree", "last"},
                dataView.getItems().toArray(String[]::new));

        dataView.addItemsBefore(Arrays.asList("newFour", "middle"),
                "middle");

        Assert.assertArrayEquals(
                new String[]{"newFour", "middle", "first", "newOne", "newTwo", "newThree", "last"},
                dataView.getItems().toArray(String[]::new));

        dataView.addItemsBefore(Collections.singletonList("newFive"), "newFour");

        Assert.assertArrayEquals(
                new String[]{"newFive", "newFour", "middle", "first", "newOne"
                        , "newTwo", "newThree", "last"},
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
        exceptionRule.expectMessage(
                "Items collection cannot be null");

        dataView.addItemsBefore(null, "any");
    }

    @Test
    public void addItemsBefore_emptyCollectionPassed_dataNotChanged() {
        dataView.addItemsBefore(Collections.emptyList(), "any");
        Assert.assertArrayEquals(
                new String[]{"first", "middle", "last"},
                dataView.getItems().toArray(String[]::new));
    }

    @Test
    public void removeItems_itemsOutOfOrder_allItemsAreRemoved() {
        dataView.removeItems(Arrays.asList("middle", "first"));

        Assert.assertArrayEquals(new String[]{"last"},
                dataView.getItems().toArray(String[]::new));
    }

    @Test
    public void removeItems_nullCollectionPassed_throwsException() {
        exceptionRule.expect(NullPointerException.class);
        exceptionRule.expectMessage(
                "Items collection cannot be null");

        dataView.removeItems(null);
    }

    @Test
    public void removeItems_emptyCollectionPassed_dataNotChanged() {
        dataView.removeItems(Collections.emptyList());
        Assert.assertArrayEquals(
                new String[]{"first", "middle", "last"},
                dataView.getItems().toArray(String[]::new));
    }

    @Test
    public void addItemsAndRemoveItems_noConcurrencyIssues() {
        dataView.addItemsBefore(Arrays.asList("newOne", "newTwo", "newThree"),
                "middle");

        Assert.assertArrayEquals(
                new String[]{"first", "newOne", "newTwo", "newThree",
                        "middle", "last"},
                dataView.getItems().toArray(String[]::new));

        dataView.removeItems(Arrays.asList("middle", "first"));

        Assert.assertArrayEquals(
                new String[]{"newOne", "newTwo", "newThree", "last"},
                dataView.getItems().toArray(String[]::new));

        dataView.addItemsAfter(Arrays.asList("one", "two"), "newOne");

        Assert.assertArrayEquals(
                new String[]{"newOne", "one", "two", "newTwo", "newThree",
                        "last"},
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

        final ListDataProvider<String> stringListDataProvider =
                new ListDataProvider<>(items);
        dataView = new ListDataViewImpl(() -> stringListDataProvider, component);

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

        final ListDataProvider<String> stringListDataProvider =
                new ListDataProvider<>(items);
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

        final ListDataProvider<String> stringListDataProvider =
                new ListDataProvider<>(items);
        dataView = new ListDataViewImpl(() -> stringListDataProvider, component);

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

        final ListDataProvider<String> stringListDataProvider =
                new ListDataProvider<>(items);
        dataView = new ListDataViewImpl(() -> stringListDataProvider, component);

        dataView.addItemsBefore(Collections.singleton("newItem"), "item1");
    }

    @Test
    public void addFilter_FilterIsAddedOnTop() {
        items = new ArrayList<>(
                Arrays.asList("item1", "item2", "item22", "item3"));
        dataProvider = DataProvider.ofCollection(items);

        Assert.assertEquals(4, dataView.getItems().count());

        dataView.addFilter(
                item -> item.equals("item1") || item.equals("item2") || item
                        .equals("item22"));

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
    public void updateItem_itemPresentInDataSet_refreshesItem() {
        Collection<Item> items = getTestItems();

        ListDataProvider<Item> dataProvider =
                Mockito.spy(DataProvider.ofCollection(items));

        ItemListDataView dataView = new ItemListDataView(
                () -> dataProvider, component);

        Iterator<Item> iterator = items.iterator();
        Item firstItem = iterator.next();
        firstItem.setValue("updatedValue");

        dataView.updateItem(firstItem);

        Mockito.verify(dataProvider).refreshItem(firstItem);

        dataView.setIdentifierProvider(Item::getId);
        Item secondItem = iterator.next();
        secondItem.setValue("updatedValue");

        Item secondItemDuplicate = new Item(2L);
        dataView.updateItem(secondItemDuplicate);
        Mockito.verify(dataProvider).refreshItem(secondItem);
    }

    @Test
    public void updateItem_itemNotPresent_itemNotRefreshed() {
        Collection<Item> items = getTestItems();

        ListDataProvider<Item> dataProvider =
                Mockito.spy(DataProvider.ofCollection(items));

        ItemListDataView dataView = new ItemListDataView(
                () -> dataProvider, component);

        Item updatedItem = new Item(42L, "updated", "descr1");
        dataView.updateItem(updatedItem);
        Mockito.verify(dataProvider, Mockito.times(0))
                .refreshItem(updatedItem);

        updatedItem = new Item(1L, "updated", "descr1");
        dataView.updateItem(updatedItem);
        Mockito.verify(dataProvider, Mockito.times(0))
                .refreshItem(updatedItem);
    }

    private static class ListDataViewImpl extends AbstractListDataView<String> {

        public ListDataViewImpl(
                SerializableSupplier<DataProvider<String, ?>> dataProviderSupplier,
                Component component) {
            super(dataProviderSupplier, component);
        }
    }

    private static class ItemListDataView extends AbstractListDataView<Item> {

        public ItemListDataView(
                SerializableSupplier<DataProvider<Item, ?>> dataProviderSupplier,
                Component component) {
            super(dataProviderSupplier, component);
        }
    }

    private Collection<Item> getTestItems() {
        return new ArrayList<>(Arrays.asList(
                new Item(1L, "value1", "descr1"),
                new Item(2L, "value2", "descr2"),
                new Item(3L, "value3", "descr3")));
    }

    @Tag("test-component")
    private static class TestComponent extends Component {
    }
}
