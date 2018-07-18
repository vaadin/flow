/*
 * Copyright 2000-2018 Vaadin Ltd.
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

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.vaadin.flow.data.provider.hierarchy.HierarchyMapper;
import com.vaadin.flow.data.provider.hierarchy.TreeData;
import com.vaadin.flow.data.provider.hierarchy.TreeDataProvider;
import com.vaadin.flow.function.SerializablePredicate;
import com.vaadin.flow.internal.Range;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class HierarchyMapperWithNumerousDataTest {

    private static final int ROOT_COUNT = 1;
    private static final int PARENT_COUNT = 100000;

    private static TreeData<Node> data = new TreeData<>();
    private TreeDataProvider<Node> provider;
    private HierarchyMapper<Node, SerializablePredicate<Node>> mapper;
    private static List<Node> testData;
    private static List<Node> roots;
    private int mapSize = ROOT_COUNT;

    @BeforeClass
    public static void setupData() {
        testData = HierarchyMapperWithDataTest.generateTestData(ROOT_COUNT,
                PARENT_COUNT, 0);
        roots = testData.stream().filter(item -> item.getParent() == null)
                .collect(Collectors.toList());
        data.addItems(roots,
                parent -> testData.stream().filter(
                        item -> Objects.equals(item.getParent(), parent))
                        .collect(Collectors.toList()));
    }

    @Before
    public void setup() {
        provider = new TreeDataProvider<>(data);
        mapper = new HierarchyMapper<>(provider);
    }

    /**
     * Test for non-logarithmic {@code getParentOfItem} implementations 100000
     * entries and 1 second should be enought to make it run even on slow
     * machines and weed out linear solutions
     */
    @Test(timeout = 1000)
    public void expandRootNode() {
        assertEquals("Map size should be equal to root node count", ROOT_COUNT,
                mapper.getTreeSize());
        expand(testData.get(0));
        assertEquals("Should be root count + once parent count",
                ROOT_COUNT + PARENT_COUNT, mapper.getTreeSize());
        checkMapSize();
    }

    private void expand(Node node) {
        insertRows(mapper.expand(node, mapper.getIndexOf(node).orElse(null)));
    }

    public void insertRows(Range range) {
        assertTrue("Index not in range",
                0 <= range.getStart() && range.getStart() <= mapSize);
        mapSize += range.length();
    }

    private void checkMapSize() {
        assertEquals("Map size not properly updated", mapper.getTreeSize(),
                mapSize);
    }
}
