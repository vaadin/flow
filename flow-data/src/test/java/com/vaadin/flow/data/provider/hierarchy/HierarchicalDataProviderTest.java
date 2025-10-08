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

import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.junit.Assert;
import org.junit.Test;

import com.vaadin.flow.data.provider.*;
import com.vaadin.flow.shared.Registration;

public class HierarchicalDataProviderTest {

    @Test
    public void getParent_throwsUnsupportedOperationException() {
        var dataProvider = new TestDataProvider();
        var rootItem = new TestBean(null, 0, 3);
        Assert.assertThrows(UnsupportedOperationException.class,
                () -> dataProvider.getParent(rootItem));
    }

    @Test
    public void inMemoryDataProvider_getNullItemIndex_throwsNullPointerException() {
        var dataProvider = new TestDataProvider();
        dataProvider.setInMemory(true);
        var query = new HierarchicalQuery<TestBean, Object>(null, null);
        Assert.assertThrows(NullPointerException.class,
                () -> dataProvider.getItemIndex(null, query));
    }

    @Test
    public void inMemoryDataProvider_getItemIndexWithNullQuery_throwsNullPointerException() {
        var dataProvider = new TestDataProvider();
        dataProvider.setInMemory(true);
        var rootItem = new TestBean(null, 0, 3);
        Assert.assertThrows(NullPointerException.class,
                () -> dataProvider.getItemIndex(rootItem, null));
    }

    @Test
    public void defaultDataProvider_getItemIndex_throwsUnsupportedOperationException() {
        var dataProvider = new TestDataProvider();
        var rootItem = new TestBean(null, 0, 3);
        Assert.assertThrows(UnsupportedOperationException.class,
                () -> dataProvider.getItemIndex(rootItem, null));
    }

    @Test
    public void inMemoryDataProvider_getItemIndex_returnsCorrectIndex() {
        var dataProvider = new TestDataProvider();
        dataProvider.setInMemory(true);
        var rootItem = new TestBean(null, 0, 3);
        var query = new HierarchicalQuery<TestBean, Object>(null, null);
        var itemIndex = dataProvider.getItemIndex(rootItem, query);
        Assert.assertEquals(3, itemIndex);
    }

    @Test
    public void inMemoryDataProvider_getChildItemIndex_returnsCorrectIndex() {
        var dataProvider = new TestDataProvider();
        dataProvider.setInMemory(true);
        var rootItem = new TestBean(null, 0, 3);
        var childItem = new TestBean(rootItem.getId(), 1, 3);
        var query = new HierarchicalQuery<>(null, rootItem);
        var itemIndex = dataProvider.getItemIndex(childItem, query);
        Assert.assertEquals(3, itemIndex);
    }

    @Test
    public void inMemoryDataProvider_getItemIndexInIncorrectParent_returnsMinusOne() {
        var dataProvider = new TestDataProvider();
        dataProvider.setInMemory(true);
        var rootItem = new TestBean(null, 0, 3);
        var anotherRootItem = new TestBean(null, 0, 4);
        var query = new HierarchicalQuery<>(null, anotherRootItem);
        var itemIndex = dataProvider.getItemIndex(rootItem, query);
        Assert.assertEquals(-1, itemIndex);
    }

    @Test
    public void inMemoryDataProvider_getItemIndexForAnIncorrectItem_returnsMinusOne() {
        var dataProvider = new TestDataProvider();
        dataProvider.setInMemory(true);
        var notPresentItem = new TestBean(null, 0, 20000);
        var query = new HierarchicalQuery<TestBean, Object>(null, null);
        var itemIndex = dataProvider.getItemIndex(notPresentItem, query);
        Assert.assertEquals(-1, itemIndex);
    }

    private static class TestDataProvider
            implements HierarchicalDataProvider<TestBean, Object> {

        private static final int DEPTH = 3;
        private static final int ROOT_ITEM_COUNT = 50;
        private static final int CHILD_PER_LEVEL = 10;

        private boolean inMemory;

        @Override
        public int getChildCount(HierarchicalQuery<TestBean, Object> query) {
            return hasChildren(query.getParent()) ? ROOT_ITEM_COUNT : 0;
        }

        @Override
        public Stream<TestBean> fetchChildren(
                HierarchicalQuery<TestBean, Object> query) {
            if (query.getParent() == null) {
                return IntStream.range(0, ROOT_ITEM_COUNT)
                        .mapToObj(i -> new TestBean(null, 0, i));
            }
            return IntStream.range(0, CHILD_PER_LEVEL)
                    .mapToObj(i -> new TestBean(query.getParent().getId(),
                            query.getParent().getDepth() + 1, i));
        }

        @Override
        public boolean hasChildren(TestBean o) {
            if (o == null) {
                return true;
            }
            return o.getDepth() < DEPTH - 1;
        }

        public void setInMemory(boolean inMemory) {
            this.inMemory = inMemory;
        }

        @Override
        public boolean isInMemory() {
            return inMemory;
        }

        @Override
        public void refreshItem(TestBean o) {
            // NO-OP
        }

        @Override
        public void refreshAll() {
            // NO-OP
        }

        @Override
        public Registration addDataProviderListener(
                DataProviderListener<TestBean> listener) {
            return null;
        }
    }

    private static class TestBean {
        private final String id;
        private final int depth;
        private final int index;

        public TestBean(String parentId, int depth, int index) {
            id = (parentId == null ? "" : parentId) + "/" + depth + "/" + index;
            this.depth = depth;
            this.index = index;
        }

        public int getDepth() {
            return depth;
        }

        public int getIndex() {
            return index;
        }

        public String getId() {
            return id;
        }

        @Override
        public String toString() {
            return depth + " | " + index;
        }

        @Override
        public int hashCode() {
            return id.hashCode();
        }

        @Override
        public boolean equals(Object obj) {
            if (obj instanceof TestBean other) {
                return id.equals(other.id);
            }
            return false;
        }
    }
}
