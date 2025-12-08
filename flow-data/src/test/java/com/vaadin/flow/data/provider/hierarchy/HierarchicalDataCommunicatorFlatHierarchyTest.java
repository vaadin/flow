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
package com.vaadin.flow.data.provider.hierarchy;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import com.vaadin.flow.data.provider.CompositeDataGenerator;
import com.vaadin.flow.data.provider.QuerySortOrder;
import com.vaadin.flow.data.provider.QuerySortOrderBuilder;
import com.vaadin.flow.data.provider.hierarchy.HierarchicalDataProvider.HierarchyFormat;
import com.vaadin.flow.function.SerializableComparator;
import com.vaadin.flow.function.SerializablePredicate;

public class HierarchicalDataCommunicatorFlatHierarchyTest
        extends AbstractHierarchicalDataCommunicatorTest {
    private TreeData<Item> treeData = new TreeData<>();

    private TreeDataProvider<Item> dataProvider = new TreeDataProvider<>(
            treeData, HierarchyFormat.FLATTENED);

    private HierarchicalDataCommunicator<Item> dataCommunicator;

    @Before
    public void init() {
        super.init();

        var compositeDataGenerator = new CompositeDataGenerator<Item>();
        compositeDataGenerator.addDataGenerator((item, json) -> {
            json.put("name", item.getName());
            json.put("state", item.getState());
        });

        dataCommunicator = new HierarchicalDataCommunicator<>(
                compositeDataGenerator, arrayUpdater, ui.getElement().getNode(),
                () -> null);
        dataCommunicator.setDataProvider(dataProvider, null);

        populateTreeData(treeData, 100, 2, 2);
    }

    @Test
    public void setViewportRange_requestedRangeSent() {
        dataCommunicator.expand(Arrays.asList(new Item("Item 0"),
                new Item("Item 0-0"), new Item("Item 99")));
        dataCommunicator.setViewportRange(0, 6);
        fakeClientCommunication();

        assertArrayUpdateSize(106);
        assertArrayUpdateRange(0, 6);
        assertArrayUpdateItems("name", Map.of( //
                0, "Item 0", //
                1, "Item 0-0", //
                2, "Item 0-0-0", //
                3, "Item 0-0-1", //
                4, "Item 0-1", //
                5, "Item 1"));

        Mockito.clearInvocations(arrayUpdater, arrayUpdate);

        dataCommunicator.setViewportRange(100, 8);
        fakeClientCommunication();

        assertArrayUpdateSize(106);
        assertArrayUpdateRange(100, 6);
        assertArrayUpdateItems("name", Map.of( //
                100, "Item 96", //
                101, "Item 97", //
                102, "Item 98", //
                103, "Item 99", //
                104, "Item 99-0", //
                105, "Item 99-1"));
    }

    @Test
    public void toggleItems_updatedRangeSent() {
        dataCommunicator.setViewportRange(10, 4);
        fakeClientCommunication();

        assertArrayUpdateSize(100);
        assertArrayUpdateRange(10, 4);
        assertArrayUpdateItems("name", Map.of( //
                10, "Item 10", //
                11, "Item 11", //
                12, "Item 12", //
                13, "Item 13"));

        Mockito.clearInvocations(arrayUpdater, arrayUpdate);

        dataCommunicator.expand(new Item("Item 10"));
        fakeClientCommunication();

        assertArrayUpdateSize(102);
        assertArrayUpdateRange(10, 4);
        assertArrayUpdateItems("name", Map.of(//
                10, "Item 10", //
                11, "Item 10-0", //
                12, "Item 10-1", //
                13, "Item 11"));

        Mockito.clearInvocations(arrayUpdater, arrayUpdate);

        dataCommunicator.collapse(new Item("Item 10"));
        fakeClientCommunication();

        assertArrayUpdateSize(100);
        assertArrayUpdateRange(10, 4);
        assertArrayUpdateItems("name", Map.of( //
                10, "Item 10", //
                11, "Item 11", //
                12, "Item 12", //
                13, "Item 13"));
    }

    @Test
    public void refreshItem_updatedRangeSent() {
        dataCommunicator.setViewportRange(0, 4);
        fakeClientCommunication();
        assertArrayUpdateItems("state", Map.of( //
                0, "initial", //
                1, "initial", //
                2, "initial", //
                3, "initial"));

        Mockito.clearInvocations(arrayUpdater, arrayUpdate);

        dataCommunicator.refresh(new Item("Item 0", "refreshed"));
        fakeClientCommunication();
        assertArrayUpdateItems("state", Map.of(0, "refreshed"));
    }

    @Test(expected = UnsupportedOperationException.class)
    public void refreshItemWithChildren_throws() {
        dataCommunicator.refresh(new Item("Item 0", "refreshed"), true);
    }

    @Test
    public void reset_updatedRangeSent() {
        dataCommunicator.expand(
                Arrays.asList(new Item("Item 1"), new Item("Item 1-0")));
        dataCommunicator.setViewportRange(0, 5);
        fakeClientCommunication();
        assertArrayUpdateItems("name", Map.of( //
                0, "Item 0", //
                1, "Item 1", //
                2, "Item 1-0", //
                3, "Item 1-0-0", //
                4, "Item 1-0-1"));

        Mockito.clearInvocations(arrayUpdater, arrayUpdate);

        treeData.removeItem(new Item("Item 0"));
        treeData.removeItem(new Item("Item 1-0"));
        dataCommunicator.reset();
        fakeClientCommunication();
        assertArrayUpdateItems("name", Map.of( //
                0, "Item 1", //
                1, "Item 1-1", //
                2, "Item 2", //
                3, "Item 3", //
                4, "Item 4"));
    }

    @Test
    public void resolveIndexPath_correctIndexReturned() {
        dataCommunicator.expand(
                Arrays.asList(new Item("Item 0"), new Item("Item 0-0")));

        Assert.assertEquals(0, dataCommunicator.resolveIndexPath(0));
        Assert.assertEquals(104, dataCommunicator.rootCache.getFlatSize());

        Assert.assertEquals(50, dataCommunicator.resolveIndexPath(50));
        Assert.assertEquals(104, dataCommunicator.rootCache.getFlatSize());

        Assert.assertEquals(54, dataCommunicator.resolveIndexPath(-50));
        Assert.assertEquals(104, dataCommunicator.rootCache.getFlatSize());

        Assert.assertEquals(0, dataCommunicator.resolveIndexPath(-104));
        Assert.assertEquals(104, dataCommunicator.rootCache.getFlatSize());
    }

    @Test
    public void invalidIndexPath_resolveIndexPath_correctIndexReturned() {
        dataCommunicator.expand(
                Arrays.asList(new Item("Item 0"), new Item("Item 0-0")));

        Assert.assertEquals(2, dataCommunicator.resolveIndexPath(2, 2, 2));
        Assert.assertEquals(104, dataCommunicator.rootCache.getFlatSize());

        Assert.assertEquals(103, dataCommunicator.resolveIndexPath(1000));
        Assert.assertEquals(104, dataCommunicator.rootCache.getFlatSize());

        Assert.assertEquals(0, dataCommunicator.resolveIndexPath(-1000));
        Assert.assertEquals(104, dataCommunicator.rootCache.getFlatSize());
    }

    @Test
    public void buildQuery_correctQueryReturned() {
        List<QuerySortOrder> sortOrders = new QuerySortOrderBuilder()
                .thenDesc("name").build();
        SerializablePredicate<Item> filter = (f) -> true;
        SerializableComparator<Item> comparator = Comparator
                .comparing(Item::getName).reversed()::compare;

        dataCommunicator.setDataProvider(dataProvider, filter);
        dataCommunicator.setInMemorySorting(comparator);
        dataCommunicator.setBackEndSorting(sortOrders);
        dataCommunicator.expand(new Item("Item 0"));

        var query = dataCommunicator.buildQuery(10, 20);
        Assert.assertNull(query.getParent());
        Assert.assertEquals(10, query.getOffset());
        Assert.assertEquals(20, query.getLimit());
        Assert.assertEquals(filter, query.getFilter().get());
        Assert.assertEquals(sortOrders, query.getSortOrders());
        Assert.assertEquals(comparator, query.getInMemorySorting());
        Assert.assertEquals(Set.of(new Item("Item 0")),
                query.getExpandedItemIds());

        query = dataCommunicator.buildQuery(new Item("Item 0"), 10, 20);
        Assert.assertEquals(new Item("Item 0"), query.getParent());
        Assert.assertEquals(10, query.getOffset());
        Assert.assertEquals(20, query.getLimit());
        Assert.assertEquals(filter, query.getFilter().get());
        Assert.assertEquals(sortOrders, query.getSortOrders());
        Assert.assertEquals(comparator, query.getInMemorySorting());
        Assert.assertEquals(Set.of(new Item("Item 0")),
                query.getExpandedItemIds());
    }
}
