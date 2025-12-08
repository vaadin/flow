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

import java.util.Collection;
import java.util.Optional;
import java.util.stream.Stream;

import org.junit.Test;

import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.data.provider.DataProvider;
import com.vaadin.flow.data.provider.DataView;
import com.vaadin.flow.data.provider.HasDataView;
import com.vaadin.flow.data.provider.IdentifierProvider;
import com.vaadin.flow.data.provider.InMemoryDataProvider;
import com.vaadin.flow.data.provider.ItemCountChangeEvent;
import com.vaadin.flow.data.provider.ItemIndexProvider;
import com.vaadin.flow.data.provider.LazyDataView;
import com.vaadin.flow.data.provider.ListDataView;
import com.vaadin.flow.data.provider.SortDirection;
import com.vaadin.flow.function.SerializableComparator;
import com.vaadin.flow.function.SerializablePredicate;
import com.vaadin.flow.function.ValueProvider;
import com.vaadin.flow.shared.Registration;

public class HasHierarchicalDataProviderTest {

    interface TestLazyDataView extends LazyDataView<String> {

    }

    interface TestListDataView extends ListDataView<String, TestListDataView> {

    }

    interface TestDataView extends DataView<String> {

    }

    // This is just to verify that the hierarchy is possible for tree grid
    public static class TestHierarchicalComponent implements TestLazyDataView,
            TestListDataView, HasHierarchicalDataProvider<String>,
            HasDataView<String, Void, TestDataView> {

        @Override
        public void setItemCountFromDataProvider() {

        }

        @Override
        public void setItemCountEstimate(int itemCountEstimate) {

        }

        @Override
        public int getItemCountEstimate() {
            return 0;
        }

        @Override
        public void setItemCountEstimateIncrease(
                int itemCountEstimateIncrease) {

        }

        @Override
        public int getItemCountEstimateIncrease() {
            return 0;
        }

        @Override
        public void setItemCountUnknown() {

        }

        @Override
        public void setItemIndexProvider(
                ItemIndexProvider<String, ?> itemIndexProvider) {

        }

        @Override
        public int getItemCount() {
            return 0;
        }

        @Override
        public Optional<String> getNextItem(String item) {
            return Optional.empty();
        }

        @Override
        public Optional<String> getPreviousItem(String item) {
            return Optional.empty();
        }

        @Override
        public TestListDataView addItem(String item) {
            return null;
        }

        @Override
        public TestListDataView addItemAfter(String item, String after) {
            return null;
        }

        @Override
        public TestListDataView addItemBefore(String item, String before) {
            return null;
        }

        @Override
        public void refreshItem(String item) {
        }

        @Override
        public void refreshAll() {

        }

        @Override
        public TestListDataView addItems(Collection<String> items) {
            return null;
        }

        @Override
        public TestListDataView addItemsAfter(Collection<String> items,
                String after) {
            return null;
        }

        @Override
        public TestListDataView addItemsBefore(Collection<String> items,
                String before) {
            return null;
        }

        @Override
        public TestListDataView removeItem(String item) {
            return null;
        }

        @Override
        public TestListDataView removeItems(Collection<String> items) {
            return null;
        }

        @Override
        public TestListDataView setItems(Collection<String> items) {
            return null;
        }

        @Override
        public TestListDataView setFilter(
                SerializablePredicate<String> filter) {
            return null;
        }

        @Override
        public TestListDataView addFilter(
                SerializablePredicate<String> filter) {
            return null;
        }

        @Override
        public TestListDataView removeFilters() {
            return null;
        }

        @Override
        public TestListDataView setSortComparator(
                SerializableComparator<String> sortComparator) {
            return null;
        }

        @Override
        public TestListDataView addSortComparator(
                SerializableComparator<String> sortComparator) {
            return null;
        }

        @Override
        public TestListDataView removeSorting() {
            return null;
        }

        @Override
        public <V1 extends Comparable<? super V1>> TestListDataView setSortOrder(
                ValueProvider<String, V1> valueProvider,
                SortDirection sortDirection) {
            return null;
        }

        @Override
        public <V1 extends Comparable<? super V1>> TestListDataView addSortOrder(
                ValueProvider<String, V1> valueProvider,
                SortDirection sortDirection) {
            return null;
        }

        @Override
        public String getItem(int index) {
            return null;
        }

        @Override
        public Optional<Integer> getItemIndex(String item) {
            return Optional.of(0);
        }

        @Override
        public Stream<String> getItems() {
            return null;
        }

        @Override
        public boolean contains(String item) {
            return false;
        }

        @Override
        public Registration addItemCountChangeListener(
                ComponentEventListener<ItemCountChangeEvent<?>> listener) {
            return null;
        }

        @Override
        public void setIdentifierProvider(
                IdentifierProvider<String> identifierProvider) {

        }

        @Override
        public HierarchicalDataProvider<String, SerializablePredicate<String>> getDataProvider() {
            return null;
        }

        @Override
        public void setDataProvider(
                HierarchicalDataProvider<String, ?> hierarchicalDataProvider) {

        }

        @Override
        public TestDataView setItems(DataProvider<String, Void> dataProvider) {
            return null;
        }

        @Override
        public TestDataView setItems(
                InMemoryDataProvider<String> dataProvider) {
            return null;
        }

        @Override
        public TestDataView getGenericDataView() {
            return null;
        }
    }

    @Test
    public void testDataView_componentHierachyIsPossible() {
        new TestHierarchicalComponent();
    }

}
