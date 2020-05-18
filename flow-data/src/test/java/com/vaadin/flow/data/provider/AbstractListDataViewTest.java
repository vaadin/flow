package com.vaadin.flow.data.provider;

import com.vaadin.flow.shared.Registration;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.Mockito;

import java.util.Arrays;
import java.util.Collection;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class AbstractListDataViewTest {

    private final static Collection<String> ITEMS = Arrays.asList(
            "first", "middle", "last");

    @Rule
    public ExpectedException exceptionRule = ExpectedException.none();

    private DataProvider dataProvider;

    private DataController<String> dataController;

    private AbstractListDataView<String> dataView;

    @Before
    public void init() {
        dataProvider = DataProvider.ofCollection(ITEMS);
        dataController = new DataControllerStub();
        dataView = new ListDataViewImpl(dataController);
    }

    @Test
    public void createListDataViewInstance_faultyDataProvider_throwsException() {
        DataProvider dataProvider = DataProvider
                .fromCallbacks(query -> Stream.of("one"), query -> 1);
        exceptionRule.expect(IllegalStateException.class);
        exceptionRule.expectMessage(
                "ListDataViewImpl only supports 'ListDataProvider' " +
                        "or it's subclasses, but was given a 'AbstractBackEndDataProvider'");
        DataController<String> dataController = Mockito.mock(DataController.class);
        Mockito.when(dataController.getDataProvider()).thenReturn(dataProvider);
        new ListDataViewImpl(dataController);
    }

    @Test
    public void hasNextItem_nextItemAvailable_nextItemFound() {
        Assert.assertTrue("First item should have next item",
                dataView.hasNextItem("first"));
        Assert.assertTrue("Item in middle should have next item",
                dataView.hasNextItem("middle"));
    }

    @Test
    public void hasNextItem_nextItemUnavailable_nextItemNotFound() {
        Assert.assertFalse("No next item for last item should be available",
                dataView.hasNextItem("last"));
    }

    @Test
    public void getNextItem_nextItemAvailable_nextItemFound() {
        Assert.assertEquals("Faulty next item",
                "last", dataView.getNextItem("middle"));
    }

    @Test
    public void getNextItem_nextItemUnavailable_nextItemNotFound() {
        Assert.assertNull("Got next item for last item",
                dataView.getNextItem("last"));
    }

    @Test
    public void hasPrevItem_prevItemAvailable_prevItemFound() {
        Assert.assertTrue("Last item should have previous item",
                dataView.hasPreviousItem("last"));
        Assert.assertTrue("Item in middle should have previous item",
                dataView.hasPreviousItem("middle"));
    }

    @Test
    public void hasPrevItem_prevItemUnavailable_prevItemNotFound() {
        Assert.assertFalse("No previous item for first item should be available",
                dataView.hasPreviousItem("first"));
    }

    @Test
    public void getPrevItem_prevItemAvailable_prevItemFound() {
        Assert.assertEquals("Item in middle should have previous item",
                "first", dataView.getPreviousItem("middle"));
    }

    @Test
    public void getPrevItem_prevItemUnavailable_prevItemNotFound() {
        Assert.assertNull("Got previous item for first index",
                dataView.getPreviousItem("first"));
    }

    @Test
    public void withFilter_filterIsSet_filteredItemsObtained() {
        Assert.assertEquals(ITEMS.size(), dataController.getDataSize());
        dataView.withFilter(item -> item.equals("first"));
        Assert.assertEquals("Filter was not applied to data size",
                1, dataController.getDataSize());
        Assert.assertEquals("Expected item is missing from filtered data",
                "first", dataController.getAllItems().findFirst().get());
    }

    @Test
    public void withFilter_filterReset_allItemsObtained() {
        ((ListDataProvider) dataProvider).setFilter(item -> item.equals("first"));
        dataView.withFilter(null);
        Assert.assertEquals("Filter reset was not applied to data size",
                ITEMS.size(), dataController.getDataSize());
        Assert.assertArrayEquals("Filter reset was not applied to data set",
                ITEMS.toArray(), dataController.getAllItems().toArray());
    }

    @Test
    public void withSortComparator_sortIsSet_sortedItemsObtained() {
        dataView.withSortComparator(String::compareTo);
        Assert.assertEquals("Unexpected data set order", "first,last,middle",
                dataController.getAllItems().collect(Collectors.joining(",")));
    }

    @Test
    public void getAllItems_noFiltersSet_allItemsObtained() {
        Stream<String> allItems = dataView.getAllItems();
        Assert.assertArrayEquals("Unexpected data set", ITEMS.toArray(),
                allItems.toArray());
    }

    @Test
    public void getDataSize_noFiltersSet_dataSizeObtained() {
        Assert.assertEquals("Unexpected size for data", ITEMS.size(),
                dataView.getDataSize());
    }

    @Test
    public void isItemPresent_itemPresentedInDataSet_itemFound() {
        Assert.assertTrue("Set item was not found in the data",
                dataView.isItemPresent("first"));
    }

    @Test
    public void isItemPresent_itemNotPresentedInDataSet_itemNotFound() {
        Assert.assertFalse("Non existent item found in data",
                dataView.isItemPresent("absent item"));
    }

    private static class ListDataViewImpl extends AbstractListDataView<String> {
        public ListDataViewImpl(DataController<String> dataController) {
            super(dataController);
        }
    }

    private class DataControllerStub implements DataController<String> {

        @Override
        public DataProvider<String, ?> getDataProvider() { return dataProvider; }

        @Override
        public Registration addSizeChangeListener(SizeChangeListener listener) { return null; }

        @Override
        public int getDataSize() { return dataProvider.size(new Query<>()); }

        @Override
        public Stream<String> getAllItems() { return dataProvider.fetch(new Query<>()); }
    }
}