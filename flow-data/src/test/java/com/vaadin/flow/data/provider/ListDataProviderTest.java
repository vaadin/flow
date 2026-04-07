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

import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

import org.apache.commons.lang3.SerializationUtils;
import org.junit.jupiter.api.Test;

import com.vaadin.flow.function.SerializableComparator;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

class ListDataProviderTest
        extends DataProviderTestBase<ListDataProvider<StrBean>> {

    @Override
    protected ListDataProvider<StrBean> createDataProvider() {
        return DataProvider.ofCollection(data);
    }

    @Test
    void setSortByProperty_ascending() {
        ListDataProvider<StrBean> dataProvider = getDataProvider();

        dataProvider.setSortOrder(StrBean::getId, SortDirection.ASCENDING);

        int[] threeFirstIds = dataProvider.fetch(new Query<>())
                .mapToInt(StrBean::getId).limit(3).toArray();

        assertArrayEquals(new int[] { 0, 1, 2 }, threeFirstIds);
    }

    @Test
    void setSortByProperty_descending() {
        ListDataProvider<StrBean> dataProvider = getDataProvider();

        dataProvider.setSortOrder(StrBean::getId, SortDirection.DESCENDING);

        int[] threeFirstIds = dataProvider.fetch(new Query<>())
                .mapToInt(StrBean::getId).limit(3).toArray();

        assertArrayEquals(new int[] { 98, 97, 96 }, threeFirstIds);
    }

    @Test
    void testMultipleSortOrder_firstAddedWins() {
        ListDataProvider<StrBean> dataProvider = getDataProvider();

        dataProvider.addSortOrder(StrBean::getValue, SortDirection.DESCENDING);
        dataProvider.addSortOrder(StrBean::getId, SortDirection.DESCENDING);

        List<StrBean> threeFirstItems = dataProvider.fetch(new Query<>())
                .limit(3).collect(Collectors.toList());

        // First one is Xyz
        assertEquals(new StrBean("Xyz", 10, 100), threeFirstItems.get(0));
        // The following are Foos ordered by id
        assertEquals(new StrBean("Foo", 93, 2), threeFirstItems.get(1));
        assertEquals(new StrBean("Foo", 91, 2), threeFirstItems.get(2));
    }

    @Test
    void setFilter() {
        dataProvider.setFilter(item -> item.getValue().equals("Foo"));

        assertEquals(36, sizeWithUnfilteredQuery());

        dataProvider.setFilter(item -> !item.getValue().equals("Foo"));

        assertEquals(64, sizeWithUnfilteredQuery(),
                "Previous filter should be reset when setting a new one");

        dataProvider.setFilter(null);

        assertEquals(100, sizeWithUnfilteredQuery(),
                "Setting filter to null should remove all filters");
    }

    @Test
    void setFilter_valueProvider() {
        dataProvider.setFilter(StrBean::getValue, "Foo"::equals);

        assertEquals(36, sizeWithUnfilteredQuery());

        dataProvider.setFilter(StrBean::getValue,
                value -> !value.equals("Foo"));

        assertEquals(64, sizeWithUnfilteredQuery(),
                "Previous filter should be reset when setting a new one");
    }

    @Test
    void setFilterEquals() {
        dataProvider.setFilterByValue(StrBean::getValue, "Foo");

        assertEquals(36, sizeWithUnfilteredQuery());

        dataProvider.setFilterByValue(StrBean::getValue, "Bar");

        assertEquals(23, sizeWithUnfilteredQuery());
    }

    @Test
    void addFilter_withPreviousFilter() {
        dataProvider.setFilterByValue(StrBean::getValue, "Foo");

        dataProvider.addFilter(item -> item.getId() > 50);

        assertEquals(17, sizeWithUnfilteredQuery(),
                "Both filters should be used");
    }

    @Test
    void addFilter_noPreviousFilter() {
        dataProvider.addFilter(item -> item.getId() > 50);

        assertEquals(48, sizeWithUnfilteredQuery());
    }

    @Test
    void addFilter_valueProvider() {
        dataProvider.setFilter(item -> item.getId() > 50);

        dataProvider.addFilter(StrBean::getValue, "Foo"::equals);

        assertEquals(17, sizeWithUnfilteredQuery(),
                "Both filters should be used");
    }

    @Test
    void addFilterEquals() {
        dataProvider.setFilter(item -> item.getId() > 50);

        dataProvider.addFilterByValue(StrBean::getValue, "Foo");

        assertEquals(17, sizeWithUnfilteredQuery(),
                "Both filters should be used");
    }

    @Test
    void addFilter_firstAddedUsedFirst() {
        dataProvider.addFilter(item -> false);
        dataProvider.addFilter(item -> {
            fail("This filter should never be invoked");
            return true;
        });

        assertEquals(0, sizeWithUnfilteredQuery());
    }

    @Test
    void combineProviderAndQueryFilters() {
        dataProvider.addFilterByValue(StrBean::getValue, "Foo");

        int size = dataProvider.size(new Query<>(item -> item.getId() > 50));

        assertEquals(17, size, "Both filters should be used");
    }

    @Test
    void providerFilterBeforeQueryFilter() {
        dataProvider.setFilter(item -> false);

        int size = dataProvider.size(new Query<>(item -> {
            fail("This filter should never be invoked");
            return true;
        }));

        assertEquals(0, size);
    }

    @Test
    void filteringBy_itemPredicate() {
        DataProvider<StrBean, String> filteringBy = dataProvider.filteringBy(
                (item, filterValue) -> item.getValue().equals(filterValue));

        assertSizeWithFilter(36, filteringBy, "Foo");
    }

    @Test
    void filteringBy_equals() {
        DataProvider<StrBean, String> filteringBy = dataProvider
                .filteringByEquals(StrBean::getValue);

        assertSizeWithFilter(36, filteringBy, "Foo");
    }

    @Test
    void filteringBy_propertyValuePredicate() {
        DataProvider<StrBean, Integer> filteringBy = dataProvider.filteringBy(
                StrBean::getId,
                (propertyValue, filterValue) -> propertyValue >= filterValue);

        assertSizeWithFilter(90, filteringBy, 10);
    }

    @Test
    void filteringBy_caseInsensitiveSubstring() {
        DataProvider<StrBean, String> filteringBy = dataProvider
                .filteringBySubstring(StrBean::getValue, Locale.ENGLISH);

        assertSizeWithFilter(36, filteringBy, "oo");
        assertSizeWithFilter(36, filteringBy, "Oo");
    }

    @Test
    void filterBy_caseInsensitivePrefix() {
        DataProvider<StrBean, String> filteringBy = dataProvider
                .filteringByPrefix(StrBean::getValue, Locale.ENGLISH);

        assertSizeWithFilter(36, filteringBy, "Fo");
        assertSizeWithFilter(36, filteringBy, "fo");
        assertSizeWithFilter(0, filteringBy, "oo");
    }

    @Override
    protected void setSortOrder(List<QuerySortOrder> sortOrder,
            Comparator<StrBean> comp) {
        SerializableComparator<StrBean> serializableComp = comp::compare;
        getDataProvider().setSortComparator(serializableComp);
    }

    @Test
    void serializableWithListeners() {
        ListDataProvider<StrBean> provider = getDataProvider();

        provider.addDataProviderListener(event -> {
        });

        // Should not throw
        SerializationUtils.serialize(provider);
    }

}
