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
package com.vaadin.flow.data.provider.hierarchy;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import com.vaadin.flow.function.SerializablePredicate;
import com.vaadin.flow.internal.Range;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class HierarchyMapperWithDataTest {

    private static final int ROOT_COUNT = 5;
    private static final int PARENT_COUNT = 4;
    private static final int LEAF_COUNT = 2;

    private TreeData<Node> data;
    private TreeDataProvider<Node> provider;
    private HierarchyMapper<Node, SerializablePredicate<Node>> mapper;
    private List<Node> testData;
    private List<Node> roots;
    private int mapSize;

    @Rule
    public ExpectedException exceptionRule = ExpectedException.none();

    private void setupData() {
        mapSize = ROOT_COUNT;
        data = new TreeData<>();
        testData = generateTestData(ROOT_COUNT, PARENT_COUNT, LEAF_COUNT);
        roots = testData.stream().filter(item -> item.getParent() == null)
                .collect(Collectors.toList());
        data.addItems(roots,
                parent -> testData.stream().filter(
                        item -> Objects.equals(item.getParent(), parent))
                        .collect(Collectors.toList()));
    }

    @Before
    public void setup() {
        setupData();

        provider = new TreeDataProvider<>(data);
        mapper = new HierarchyMapper<>(provider);
    }

    @Test
    public void expandRootNode() {
        assertEquals("Map size should be equal to root node count", ROOT_COUNT,
                mapper.getTreeSize());
        expand(testData.get(0));
        assertEquals("Should be root count + once parent count",
                ROOT_COUNT + PARENT_COUNT, mapper.getTreeSize());
        checkMapSize();
    }

    @Test
    public void expandAndCollapseLastRootNode() {
        assertEquals("Map size should be equal to root node count", ROOT_COUNT,
                mapper.getTreeSize());
        expand(roots.get(roots.size() - 1));
        assertEquals("Should be root count + once parent count",
                ROOT_COUNT + PARENT_COUNT, mapper.getTreeSize());
        checkMapSize();
        collapse(roots.get(roots.size() - 1));
        assertEquals("Map size should be equal to root node count again",
                ROOT_COUNT, mapper.getTreeSize());
        checkMapSize();
    }

    @Test
    public void expandHiddenNode() {
        assertEquals("Map size should be equal to root node count", ROOT_COUNT,
                mapper.getTreeSize());
        expand(testData.get(1));
        assertEquals("Map size should not change when expanding a hidden node",
                ROOT_COUNT, mapper.getTreeSize());
        checkMapSize();
        expand(roots.get(0));
        assertEquals("Hidden node should now be expanded as well",
                ROOT_COUNT + PARENT_COUNT + LEAF_COUNT, mapper.getTreeSize());
        checkMapSize();
        collapse(roots.get(0));
        assertEquals("Map size should be equal to root node count", ROOT_COUNT,
                mapper.getTreeSize());
        checkMapSize();
    }

    @Test
    public void expandLeafNode() {
        assertEquals("Map size should be equal to root node count", ROOT_COUNT,
                mapper.getTreeSize());
        expand(testData.get(0));
        expand(testData.get(1));
        assertEquals("Root and parent node expanded",
                ROOT_COUNT + PARENT_COUNT + LEAF_COUNT, mapper.getTreeSize());
        checkMapSize();
        expand(testData.get(2));
        assertEquals("Expanding a leaf node should have no effect",
                ROOT_COUNT + PARENT_COUNT + LEAF_COUNT, mapper.getTreeSize());
        checkMapSize();
    }

    @Test
    public void findParentIndexOfLeaf() {
        expand(testData.get(0));
        assertEquals("Could not find the root node of a parent",
                Integer.valueOf(0), mapper.getParentIndex(testData.get(1)));

        expand(testData.get(1));
        assertEquals("Could not find the parent of a leaf", Integer.valueOf(1),
                mapper.getParentIndex(testData.get(2)));
    }

    @Test
    public void fetchRangeOfRows() {
        expand(testData.get(0));
        expand(testData.get(1));

        List<Node> expectedResult = testData.stream()
                .filter(n -> roots.contains(n)
                        || n.getParent().equals(testData.get(0))
                        || n.getParent().equals(testData.get(1)))
                .collect(Collectors.toList());

        // Range containing deepest level of expanded nodes without their
        // parents in addition to root nodes at the end.
        Range range = Range.between(3, mapper.getTreeSize());
        verifyFetchIsCorrect(expectedResult, range);

        // Only the expanded two nodes, nothing more.
        range = Range.between(0, 2);
        verifyFetchIsCorrect(expectedResult, range);

        // Fetch everything
        range = Range.between(0, mapper.getTreeSize());
        verifyFetchIsCorrect(expectedResult, range);
    }

    @Test
    public void fetchRangeOfRowsWithSorting() {
        // Expand before sort
        expand(testData.get(0));
        expand(testData.get(1));

        // Construct a sorted version of test data with correct filters
        List<List<Node>> levels = new ArrayList<>();
        Comparator<Node> comparator = Comparator.comparing(Node::getNumber)
                .reversed();
        levels.add(testData.stream().filter(n -> n.getParent() == null)
                .sorted(comparator).collect(Collectors.toList()));
        levels.add(
                testData.stream().filter(n -> n.getParent() == testData.get(0))
                        .sorted(comparator).collect(Collectors.toList()));
        levels.add(
                testData.stream().filter(n -> n.getParent() == testData.get(1))
                        .sorted(comparator).collect(Collectors.toList()));

        List<Node> expectedResult = levels.get(0).stream().flatMap(root -> {
            Stream<Node> nextLevel = levels.get(1).stream()
                    .filter(n -> n.getParent() == root)
                    .flatMap(node -> Stream.concat(Stream.of(node),
                            levels.get(2).stream()
                                    .filter(n -> n.getParent() == node)));
            return Stream.concat(Stream.of(root), nextLevel);
        }).collect(Collectors.toList());

        // Apply sorting
        mapper.setInMemorySorting(comparator::compare);

        // Range containing deepest level of expanded nodes without their
        // parents in addition to root nodes at the end.
        Range range = Range.between(8, mapper.getTreeSize());
        verifyFetchIsCorrect(expectedResult, range);

        // Only the root nodes, nothing more.
        range = Range.between(0, ROOT_COUNT);
        verifyFetchIsCorrect(expectedResult, range);

        // Fetch everything
        range = Range.between(0, mapper.getTreeSize());
        verifyFetchIsCorrect(expectedResult, range);
    }

    @Test
    public void fetchWithFilter() {
        expand(testData.get(0));
        Node expandedNode = testData.get(2 + LEAF_COUNT); // Expand second node
        expand(expandedNode);

        SerializablePredicate<Node> filter = n -> n.getNumber() % 2 == 0;

        // Root nodes plus children of expanded nodes 0 and 4 that match the
        // filter
        List<Node> expectedResult = IntStream
            .of(0, 1, 4, 6, 7, 10, 13, 26, 39, 52).mapToObj(testData::get)
            .collect(Collectors.toList());

        mapper.setFilter(filter);

        // Fetch everything
        Range range = Range.between(0, mapper.getTreeSize());
        verifyFetchIsCorrect(expectedResult, range);
    }

    @Test
    public void getExpandedItems_expandSomeItems_returnsCorrectExpandedItems() {

        TreeNode root = new TreeNode("root", null);
        TreeNode second1 = new TreeNode("second-1", root);
        TreeNode second2 = new TreeNode("second-2", root);
        TreeNode third11 = new TreeNode("third-1-1", second1);
        TreeNode third21 = new TreeNode("third-2-1", second2);

        HierarchicalDataProvider<TreeNode, Void> dataProvider =
                new ThreeLevelStaticHierarchicalDataProvider(root,
                        new TreeNode[]{second1, second2},
                        new TreeNode[]{third11, third21});

        HierarchyMapper<TreeNode, Void> hierarchyMapper = new HierarchyMapper<>(
                dataProvider
        );

        Collection<TreeNode> expandedItems = hierarchyMapper.getExpandedItems();
        Assert.assertNotNull(expandedItems);
        Assert.assertEquals(0L, expandedItems.size());

        hierarchyMapper.expand(root);
        hierarchyMapper.expand(second2);

        expandedItems = hierarchyMapper.getExpandedItems();
        Assert.assertNotNull(expandedItems);
        Assert.assertEquals(2L, expandedItems.size());
        Assert.assertArrayEquals(new Object[]{"root", "second-2"},
                expandedItems.stream()
                        .map(TreeNode::getName)
                        .sorted().toArray());
    }

    @Test
    public void getExpandedItems_tryToAddItemsToCollection_shouldThrowException() {

        exceptionRule.expect(UnsupportedOperationException.class);

        TreeNode root = new TreeNode("root", null);
        TreeNode second1 = new TreeNode("second-1", root);
        TreeNode second2 = new TreeNode("second-2", root);
        TreeNode third11 = new TreeNode("third-1-1", second1);
        TreeNode third21 = new TreeNode("third-2-1", second2);

        HierarchicalDataProvider<TreeNode, Void> dataProvider =
                new ThreeLevelStaticHierarchicalDataProvider(root,
                        new TreeNode[]{second1, second2},
                        new TreeNode[]{third11, third21});

        HierarchyMapper<TreeNode, Void> hierarchyMapper = new HierarchyMapper<>(
                dataProvider
        );

        hierarchyMapper.expand(root);
        hierarchyMapper.expand(second1);

        Collection<TreeNode> expandedItems = hierarchyMapper.getExpandedItems();
        expandedItems.add(new TreeNode("third-1"));
    }

    private void expand(Node node) {
        insertRows(mapper.expand(node, mapper.getIndexOf(node).orElse(null)));
    }

    private void collapse(Node node) {
        removeRows(mapper.collapse(node, mapper.getIndexOf(node).orElse(null)));
    }

    private void verifyFetchIsCorrect(List<Node> expectedResult, Range range) {
        List<Node> collect = mapper.fetchHierarchyItems(range)
                .collect(Collectors.toList());
        for (int i = 0; i < range.length(); ++i) {
            assertEquals("Unexpected fetch results.",
                    expectedResult.get(i + range.getStart()), collect.get(i));
        }
    }

    static List<Node> generateTestData(int rootCount, int parentCount,
            int leafCount) {
        int counter = 0;
        List<Node> nodes = new ArrayList<>();
        for (int i = 0; i < rootCount; ++i) {
            Node root = new Node(counter++);
            nodes.add(root);
            for (int j = 0; j < parentCount; ++j) {
                Node parent = new Node(counter++, root);
                nodes.add(parent);
                for (int k = 0; k < leafCount; ++k) {
                    nodes.add(new Node(counter++, parent));
                }
            }
        }
        return nodes;
    }

    private void checkMapSize() {
        assertEquals("Map size not properly updated", mapper.getTreeSize(),
                mapSize);
    }

    public void removeRows(Range range) {
        assertTrue("Index not in range",
                0 <= range.getStart() && range.getStart() < mapSize);
        assertTrue("Removing more items than in map",
                range.getEnd() <= mapSize);
        mapSize -= range.length();
    }

    public void insertRows(Range range) {
        assertTrue("Index not in range",
                0 <= range.getStart() && range.getStart() <= mapSize);
        mapSize += range.length();
    }

    private static class TreeNode {
        private String name;
        private TreeNode parent;

        public TreeNode(String name) {
            this.name = name;
        }

        public TreeNode(String name, TreeNode parent) {
            this.name = name;
            this.parent = parent;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public TreeNode getParent() {
            return parent;
        }

        public void setParent(TreeNode parent) {
            this.parent = parent;
        }
    }

    private static class ThreeLevelStaticHierarchicalDataProvider
            extends AbstractBackEndHierarchicalDataProvider<TreeNode, Void> {

        private TreeNode root;
        private TreeNode[] secondLevelNodes;
        private TreeNode[] thirdLevelNodes;

        public ThreeLevelStaticHierarchicalDataProvider(
                TreeNode root,
                TreeNode[] secondLevelNodes,
                TreeNode[] thirdLevelNodes) {
            this.root = root;
            this.secondLevelNodes = secondLevelNodes;
            this.thirdLevelNodes = thirdLevelNodes;
        }

        @Override
        public int getChildCount(HierarchicalQuery<TreeNode, Void> query) {
            // query node is the root:
            if (query.getParent() == null) {
                return secondLevelNodes.length;
            }
            // query node is among the last(third) layer:
            if (Arrays.stream(secondLevelNodes)
                    .anyMatch(node -> node == query.getParent())) {
                return 0;
            }
            // count nodes of last(third) layer that are children of query's
            // parent:
            return (int) Arrays.stream(thirdLevelNodes)
                    .filter(node -> node.getParent() == query.getParent())
                    .count();
        }

        @Override
        public boolean hasChildren(TreeNode item) {
            return item.getParent() == null ||
                    Arrays.stream(secondLevelNodes)
                            .anyMatch(node -> node == item);
        }

        @Override
        protected Stream<TreeNode> fetchChildrenFromBackEnd(
                HierarchicalQuery<TreeNode, Void> query) {
            if (query.getParent() == null) {
                return Arrays.stream(new TreeNode[] {root});
            }
            if(query.getParent() == root) {
                return Arrays.stream(secondLevelNodes);
            }
            return Arrays.stream(thirdLevelNodes)
                    .filter(node -> node.getParent() == query.getParent());
        }
    }

}
