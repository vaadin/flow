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
package com.vaadin.flow.data.provider.hierarchy;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.vaadin.flow.data.provider.DataProviderTestBase;
import com.vaadin.flow.data.provider.QuerySortOrder;
import com.vaadin.flow.data.provider.StrBean;
import com.vaadin.flow.function.SerializablePredicate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TreeDataProviderTest
        extends DataProviderTestBase<TreeDataProvider<StrBean>> {

    private TreeData<StrBean> data;
    private List<StrBean> flattenedData;
    private List<StrBean> rootData;

    @BeforeEach
    @Override
    public void setUp() {
        List<StrBean> randomBeans = StrBean.generateRandomBeans(20);
        flattenedData = new ArrayList<>();
        rootData = new ArrayList<>();

        data = new TreeData<>();
        data.addItems(null, randomBeans.subList(0, 5));
        data.addItems(randomBeans.get(0), randomBeans.subList(5, 10));
        data.addItems(randomBeans.get(5), randomBeans.subList(10, 15));
        data.addItems(null, randomBeans.subList(15, 20));

        flattenedData.add(randomBeans.get(0));
        flattenedData.add(randomBeans.get(5));
        flattenedData.addAll(randomBeans.subList(10, 15));
        flattenedData.addAll(randomBeans.subList(6, 10));
        flattenedData.addAll(randomBeans.subList(1, 5));
        flattenedData.addAll(randomBeans.subList(15, 20));

        rootData.addAll(randomBeans.subList(0, 5));
        rootData.addAll(randomBeans.subList(15, 20));

        super.setUp();
    }

    @Test
    void treeData_add_item_parent_not_in_hierarchy_throws() {
        assertThrows(IllegalArgumentException.class, () -> new TreeData<>()
                .addItem(new StrBean("", 0, 0), new StrBean("", 0, 0)));
    }

    @Test
    void treeData_add_null_item_throws() {
        assertThrows(NullPointerException.class,
                () -> new TreeData<>().addItem(null, null));
    }

    @Test
    void treeData_add_item_already_in_hierarchy_throws() {
        assertThrows(IllegalArgumentException.class, () -> {
            StrBean bean = new StrBean("", 0, 0);
            new TreeData<>().addItem(null, bean).addItem(null, bean);
        });
    }

    @Test
    void treeData_remove_root_item() {
        data.removeItem(null);
        assertTrue(data.getChildren(null).isEmpty());
    }

    @Test
    void treeData_clear() {
        data.clear();
        assertTrue(data.getChildren(null).isEmpty());
    }

    @Test
    void treeData_re_add_removed_item() {
        StrBean item = rootData.get(0);
        data.removeItem(item).addItem(null, item);
        assertTrue(data.getChildren(null).contains(item));
    }

    @Test
    void treeData_get_parent() {
        StrBean root = rootData.get(0);
        StrBean firstChild = data.getChildren(root).get(0);
        assertNull(data.getParent(root));
        assertEquals(root, data.getParent(firstChild));
    }

    @Test
    void treeData_set_parent() {
        StrBean item1 = rootData.get(0);
        StrBean item2 = rootData.get(1);
        assertEquals(0, data.getChildren(item2).size());
        assertEquals(10, data.getRootItems().size());

        // Move item1 as item2's child
        data.setParent(item1, item2);
        assertEquals(1, data.getChildren(item2).size());
        assertEquals(9, data.getRootItems().size());
        assertEquals(item1, data.getChildren(item2).get(0));

        // Move back to root
        data.setParent(item1, null);
        assertEquals(0, data.getChildren(item2).size());
        assertEquals(10, data.getRootItems().size());
    }

    @Test
    void treeData_move_after_sibling() {
        StrBean root0 = rootData.get(0);
        StrBean root9 = rootData.get(9);
        assertEquals(root0, data.getRootItems().get(0));
        assertEquals(root9, data.getRootItems().get(9));

        // Move to last position
        data.moveAfterSibling(root0, root9);
        assertEquals(root0, data.getRootItems().get(9));
        assertEquals(root9, data.getRootItems().get(8));

        // Move back to first position
        data.moveAfterSibling(root0, null);
        assertEquals(root0, data.getRootItems().get(0));
        assertEquals(root9, data.getRootItems().get(9));

        StrBean child0 = data.getChildren(root0).get(0);
        StrBean child2 = data.getChildren(root0).get(2);

        // Move first child to different position
        data.moveAfterSibling(child0, child2);
        assertEquals(2, data.getChildren(root0).indexOf(child0));
        assertEquals(1, data.getChildren(root0).indexOf(child2));

        // Move child back to first position
        data.moveAfterSibling(child0, null);
        assertEquals(0, data.getChildren(root0).indexOf(child0));
        assertEquals(2, data.getChildren(root0).indexOf(child2));
    }

    @Test
    void treeData_move_after_sibling_different_parents() {
        assertThrows(IllegalArgumentException.class, () -> {
            StrBean root0 = rootData.get(0);
            StrBean wrongSibling = data.getChildren(root0).get(0);

            data.moveAfterSibling(root0, wrongSibling);
        });
    }

    @Test
    void treeData_setParent_direct_cycle_throws() {
        // Test direct cycle: parent -> child, then try to set child as parent's
        // parent
        StrBean parent = rootData.get(0);
        StrBean child = data.getChildren(parent).get(0);

        // This should throw because it would create cycle: child -> parent ->
        // child
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> data.setParent(parent, child));
        assertTrue(exception.getMessage().contains("would create a cycle"));
        assertTrue(exception.getMessage().contains(parent.toString()));
        assertTrue(exception.getMessage().contains(child.toString()));
    }

    @Test
    void treeData_setParent_multi_level_cycle_throws() {
        // Test multi-level cycle: A -> B -> C, then try to set C as A's parent
        StrBean itemA = rootData.get(0);
        StrBean itemB = data.getChildren(itemA).get(0);
        StrBean itemC = data.getChildren(itemB).get(0);

        // This should throw because it would create cycle: C -> A -> B -> C
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> data.setParent(itemA, itemC));
        assertTrue(exception.getMessage().contains("would create a cycle"));
        assertTrue(exception.getMessage().contains(itemA.toString()));
        assertTrue(exception.getMessage().contains(itemC.toString()));
    }

    @Test
    void treeData_root_items() {
        TreeData<String> data = new TreeData<>();
        TreeData<String> dataVarargs = new TreeData<>();
        TreeData<String> dataCollection = new TreeData<>();
        TreeData<String> dataStream = new TreeData<>();

        data.addItems(null, "a", "b", "c");
        dataVarargs.addRootItems("a", "b", "c");
        dataCollection.addRootItems(Arrays.asList("a", "b", "c"));
        dataStream.addRootItems(Arrays.asList("a", "b", "c").stream());

        assertEquals(data.getRootItems(), dataVarargs.getRootItems());
        assertEquals(data.getRootItems(), dataCollection.getRootItems());
        assertEquals(data.getRootItems(), dataStream.getRootItems());
    }

    @Test
    void populate_treeData_with_child_item_provider() {
        TreeData<String> stringData = new TreeData<>();
        List<String> rootItems = Arrays.asList("a", "b", "c");
        stringData.addItems(rootItems, item -> {
            if (item.length() >= 3 || item.startsWith("c")) {
                return Arrays.asList();
            }
            return Arrays.asList(item + "/a", item + "/b", item + "/c");
        });
        assertEquals(stringData.getChildren("a"),
                Arrays.asList("a/a", "a/b", "a/c"));
        assertEquals(stringData.getChildren("b"),
                Arrays.asList("b/a", "b/b", "b/c"));
        assertEquals(stringData.getChildren("c"), Arrays.asList());
        assertEquals(stringData.getChildren("a/b"), Arrays.asList());
    }

    @Test
    void populate_treeData_with_stream_child_item_provider() {
        TreeData<String> stringData = new TreeData<>();
        Stream<String> rootItems = Stream.of("a", "b", "c");
        stringData.addItems(rootItems, item -> {
            if (item.length() >= 3 || item.startsWith("c")) {
                return Stream.empty();
            }
            return Stream.of(item + "/a", item + "/b", item + "/c");
        });
        assertEquals(stringData.getChildren("a"),
                Arrays.asList("a/a", "a/b", "a/c"));
        assertEquals(stringData.getChildren("b"),
                Arrays.asList("b/a", "b/b", "b/c"));
        assertEquals(stringData.getChildren("c"), Arrays.asList());
        assertEquals(stringData.getChildren("a/b"), Arrays.asList());
    }

    @Test
    void filter_is_applied_to_children_provider_filter() {
        final SerializablePredicate<String> dataProviderFilter = item -> item
                .contains("Sub");
        final HierarchicalQuery<String, SerializablePredicate<String>> query = new HierarchicalQuery<>(
                null, null);
        filter_is_applied_to_children(dataProviderFilter, query);
    }

    @Test
    void filter_is_applied_to_children_query_filter() {
        final SerializablePredicate<String> dataProviderFilter = null;
        final HierarchicalQuery<String, SerializablePredicate<String>> query = new HierarchicalQuery<>(
                item -> item.contains("Sub"), null);
        filter_is_applied_to_children(dataProviderFilter, query);
    }

    @Test
    void filter_is_applied_to_children_both_filters() {
        final SerializablePredicate<String> dataProviderFilter = item -> item
                .contains("Sub");
        final HierarchicalQuery<String, SerializablePredicate<String>> query = new HierarchicalQuery<>(
                dataProviderFilter, null);
        filter_is_applied_to_children(dataProviderFilter, query);
    }

    private void filter_is_applied_to_children(
            final SerializablePredicate<String> dataProviderFilter,
            final HierarchicalQuery<String, SerializablePredicate<String>> query) {
        final TreeData<String> stringData = new TreeData<>();
        final String root = "Main";
        final List<String> children = Arrays.asList("Sub1", "Sub2");
        stringData.addRootItems(root);
        stringData.addItems(root, children);
        final TreeDataProvider<String> provider = new TreeDataProvider<>(
                stringData);
        provider.setFilter(dataProviderFilter);
        assertEquals(1, provider.getChildCount(query));
        assertTrue(provider.fetchChildren(query).allMatch(root::equals));
    }

    @Test
    void setFilter() {
        getDataProvider().setFilter(item -> item.getValue().equals("Xyz")
                || item.getValue().equals("Baz"));

        assertEquals(10, sizeWithUnfilteredQuery());

        getDataProvider().setFilter(item -> !item.getValue().equals("Foo")
                && !item.getValue().equals("Xyz"));

        assertEquals(14, sizeWithUnfilteredQuery(),
                "Previous filter should be replaced when setting a new one");

        getDataProvider().setFilter(null);

        assertEquals(20, sizeWithUnfilteredQuery(),
                "Setting filter to null should remove all filters");
    }

    @Test
    void addFilter() {
        getDataProvider().addFilter(item -> item.getId() <= 10);
        getDataProvider().addFilter(item -> item.getId() >= 5);
        assertEquals(8, sizeWithUnfilteredQuery());
    }

    @Test
    void rootItem_getParent_returnsNull() {
        var rootItem = rootData.get(0);
        assertNull(getDataProvider().getParent(rootItem));
    }

    @Test
    void childItem_getParent_returnsParent() {
        var rootItem = rootData.get(0);
        var childItem = data.getChildren(rootItem).get(0);
        assertEquals(rootItem, data.getParent(childItem));
    }

    @Test
    void notPresentItem_getParent_returnsNull() {
        var itemNotPresentInProvider = new StrBean("Not present", -1, 0);
        assertNull(getDataProvider().getParent(itemNotPresentInProvider));
    }

    @Test
    @Override
    public void filteringListDataProvider_convertFilter() {
        HierarchicalDataProvider<StrBean, String> strFilterDataProvider = getDataProvider()
                .withConvertedFilter(
                        text -> strBean -> strBean.getValue().contains(text));
        assertEquals(1,
                strFilterDataProvider
                        .size(new HierarchicalQuery<>("Xyz", null)),
                "Only one item should match 'Xyz'");
        assertEquals(0,
                strFilterDataProvider
                        .size(new HierarchicalQuery<>("Zyx", null)),
                "No item should match 'Zyx'");
        assertEquals(4,
                strFilterDataProvider
                        .size(new HierarchicalQuery<>("Foo", null)),
                "Unexpected number of matches for 'Foo'");
        assertEquals(rootData.size(),
                strFilterDataProvider.size(new HierarchicalQuery<>(null, null)),
                "No items should've been filtered out");
    }

    @Test
    @Override
    public void filteringListDataProvider_configurableFilter() {
        HierarchicalConfigurableFilterDataProvider<StrBean, Void, SerializablePredicate<StrBean>> configurableFilterDataProvider = getDataProvider()
                .withConfigurableFilter();

        configurableFilterDataProvider
                .setFilter(bean -> bean.getValue().contains("Xyz"));

        assertEquals(1,
                configurableFilterDataProvider
                        .size(new HierarchicalQuery<StrBean, Void>(null, null)),
                "Only one item should match 'Xyz'");

        configurableFilterDataProvider
                .setFilter(bean -> bean.getValue().contains("Zyx"));

        assertEquals(0,
                configurableFilterDataProvider
                        .size(new HierarchicalQuery<StrBean, Void>(null, null)),
                "No item should match 'Zyx'");

        configurableFilterDataProvider
                .setFilter(bean -> bean.getValue().contains("Foo"));

        assertEquals(4,
                configurableFilterDataProvider
                        .size(new HierarchicalQuery<StrBean, Void>(null, null)),
                "Unexpected number of matches for 'Foo'");

        configurableFilterDataProvider
                .setFilter(bean -> bean.getValue() == null);

        assertEquals(0,
                configurableFilterDataProvider
                        .size(new HierarchicalQuery<StrBean, Void>(null, null)),
                "No items should've been filtered out");
    }

    @Test
    @Override
    public void filteringListDataProvider_defaultFilterType() {
        assertEquals(1,
                getDataProvider().size(new HierarchicalQuery<>(
                        strBean -> strBean.getValue().contains("Xyz"), null)),
                "Only one item should match 'Xyz'");
        assertEquals(0,
                dataProvider.size(new HierarchicalQuery<>(
                        strBean -> strBean.getValue().contains("Zyx"), null)),
                "No item should match 'Zyx'");
        assertEquals(4,
                getDataProvider()
                        .size(new HierarchicalQuery<>(fooFilter, null)),
                "Unexpected number of matches for 'Foo'");
    }

    @Test
    @Override
    public void testDefaultSortWithSpecifiedPostSort() {
        Comparator<StrBean> comp = Comparator.comparing(StrBean::getValue)
                .thenComparing(Comparator.comparing(StrBean::getId).reversed());
        setSortOrder(QuerySortOrder.asc("value").thenDesc("id").build(), comp);

        List<StrBean> list = getDataProvider()
                .fetch(createQuery(QuerySortOrder.asc("randomNumber").build(),
                        Comparator.comparing(StrBean::getRandomNumber), null,
                        null))
                .collect(Collectors.toList());

        assertEquals(
                getDataProvider().fetch(new HierarchicalQuery<>(null, null))
                        .count(),
                list.size(), "Sorted data and original data sizes don't match");

        for (int i = 1; i < list.size(); ++i) {
            StrBean prev = list.get(i - 1);
            StrBean cur = list.get(i);
            // Test specific sort
            assertTrue(prev.getRandomNumber() <= cur.getRandomNumber(),
                    "Failure: " + prev.getRandomNumber() + " > "
                            + cur.getRandomNumber());

            if (prev.getRandomNumber() == cur.getRandomNumber()) {
                // Test default sort
                assertTrue(prev.getValue().compareTo(cur.getValue()) <= 0);
                if (prev.getValue().equals(cur.getValue())) {
                    assertTrue(prev.getId() > cur.getId());
                }
            }
        }
    }

    @Test
    @Override
    public void testDefaultSortWithFunction() {
        setSortOrder(QuerySortOrder.asc("value").build(),
                Comparator.comparing(StrBean::getValue));

        List<StrBean> list = getDataProvider()
                .fetch(new HierarchicalQuery<>(null, null))
                .collect(Collectors.toList());

        assertEquals(rootData.size(), list.size(),
                "Sorted data and original data sizes don't match");

        for (int i = 1; i < list.size(); ++i) {
            StrBean prev = list.get(i - 1);
            StrBean cur = list.get(i);

            // Test default sort
            assertTrue(prev.getValue().compareTo(cur.getValue()) <= 0);
        }
    }

    @Test
    @Override
    public void testListContainsAllData() {
        assertHierarchyCorrect();
    }

    @Test
    @Override
    public void testSortByComparatorListsDiffer() {
        Comparator<StrBean> comp = Comparator.comparing(StrBean::getValue)
                .thenComparing(StrBean::getRandomNumber)
                .thenComparing(StrBean::getId);

        List<StrBean> list = getDataProvider().fetch(
                createQuery(QuerySortOrder.asc("value").thenAsc("randomNumber")
                        .thenAsc("id").build(), comp, null, null))
                .collect(Collectors.toList());

        assertNotEquals(rootData.get(0), list.get(0),
                "First value should not match");

        assertEquals(rootData.size(), list.size(),
                "Sorted data and original data sizes don't match");

        rootData.sort(comp);
        for (int i = 0; i < rootData.size(); ++i) {
            assertEquals(rootData.get(i), list.get(i),
                    "Sorting result differed");
        }
    }

    @Override
    protected TreeDataProvider<StrBean> createDataProvider() {
        return new TreeDataProvider<>(data);
    }

    @Override
    protected void setSortOrder(List<QuerySortOrder> sortOrder,
            Comparator<StrBean> comp) {
        getDataProvider().setSortComparator(comp::compare);
    }

    @Override
    protected long sizeWithUnfilteredQuery() {
        return getFlattenedDataFromProvider(new ArrayList<>(), null).size();
    }

    private void assertHierarchyCorrect() {
        assertEquals(flattenedData, getFlattenedData(new ArrayList<>(), null));
        assertEquals(flattenedData,
                getFlattenedDataFromProvider(new ArrayList<>(), null));
    }

    private List<StrBean> getFlattenedData(List<StrBean> flattened,
            StrBean item) {
        if (item != null) {
            flattened.add(item);
        }
        data.getChildren(item)
                .forEach(child -> getFlattenedData(flattened, child));
        return flattened;
    }

    private List<StrBean> getFlattenedDataFromProvider(List<StrBean> flattened,
            StrBean item) {
        if (item != null) {
            flattened.add(item);
        }
        getDataProvider().fetchChildren(new HierarchicalQuery<>(null, item))
                .forEach(child -> getFlattenedDataFromProvider(flattened,
                        child));
        return flattened;
    }

    private HierarchicalQuery<StrBean, SerializablePredicate<StrBean>> createQuery(
            List<QuerySortOrder> sortOrder, Comparator<StrBean> comp,
            SerializablePredicate<StrBean> filter, StrBean parent) {
        return new HierarchicalQuery<>(0, Integer.MAX_VALUE, sortOrder, comp,
                filter, parent);
    }
}
