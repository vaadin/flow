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

import java.io.Serializable;
import java.util.Collection;
import java.util.Objects;
import java.util.stream.Stream;

import com.vaadin.flow.function.SerializablePredicate;
import com.vaadin.flow.function.ValueProvider;

/**
 * A generic interface for hierarchical listing components that use a data
 * provider for showing hierarchical data.
 *
 * @author Vaadin Ltd
 *
 * @param <T>
 *            the item data type
 * @since 1.2
 */
public interface HasHierarchicalDataProvider<T> extends Serializable {

    public HierarchicalDataProvider<T, SerializablePredicate<T>> getDataProvider();

    /**
     * Sets a new {@link TreeDataProvider} wrapping the given {@link TreeData}.
     *
     * @param treeData
     *            the tree data to set
     */
    public default void setTreeData(TreeData<T> treeData) {
        setDataProvider(new TreeDataProvider<>(treeData));
    }

    /**
     * Gets the backing {@link TreeData} instance of the data provider, if the
     * data provider is a {@link TreeDataProvider}.
     *
     * @return the TreeData instance used by the data provider
     * @throws IllegalStateException
     *             if the type of the data provider is not
     *             {@link TreeDataProvider}
     */
    public default TreeData<T> getTreeData() {
        if (getDataProvider() instanceof TreeDataProvider) {
            return ((TreeDataProvider<T>) getDataProvider()).getTreeData();
        } else {
            throw new IllegalStateException(
                    "Data provider is not an instance of TreeDataProvider");
        }
    }

    /**
     * Sets the root data items of this component provided as a collection and
     * recursively populates them with child items with the given value
     * provider.
     * <p>
     * The provided items are wrapped into a {@link TreeDataProvider} backed by
     * a flat {@link TreeData} structure. The data provider instance is used as
     * a parameter for the {@link #setDataProvider(HierarchicalDataProvider)}
     * method. It means that the items collection can be accessed later on via
     * {@link #getTreeData()}:
     *
     * <pre>
     * <code>
     * Collection&lt;Person&gt; grandParents = getGrandParents();
     * HasHierarchicalDataProvider&lt;Person&gt; treeGrid = new TreeGrid&lt;&gt;();
     * treeGrid.setItems(grandParents, Person::getChildren);
     * ...
     *
     * TreeData&lt;Person&gt; data = treeGrid.getTreeData();
     * </code>
     * </pre>
     * <p>
     * The returned {@link TreeData} instance may be used as-is to add, remove
     * or modify items in the hierarchy. These modifications to the object are
     * not automatically reflected back to the TreeGrid. Items modified should
     * be refreshed with {@link HierarchicalDataProvider#refreshItem(Object)}
     * and when adding or removing items
     * {@link HierarchicalDataProvider#refreshAll()} should be called.
     *
     * @param rootItems
     *            the root items to display, not {@code null}
     * @param childItemProvider
     *            the value provider used to recursively populate the given root
     *            items with child items, not {@code null}
     */
    public default void setItems(Collection<T> rootItems,
            ValueProvider<T, Collection<T>> childItemProvider) {
        Objects.requireNonNull(rootItems, "Given root items may not be null");
        Objects.requireNonNull(childItemProvider,
                "Given child item provider may not be null");
        setDataProvider(new TreeDataProvider<>(
                new TreeData<T>().addItems(rootItems, childItemProvider)));
    }

    /**
     * Sets the root data items of this component provided as a stream and
     * recursively populates them with child items with the given value
     * provider.
     * <p>
     * The provided items are wrapped into a {@link TreeDataProvider} backed by
     * a flat {@link TreeData} structure. The data provider instance is used as
     * a parameter for the {@link #setDataProvider(HierarchicalDataProvider)}
     * method. It means that the items collection can be accessed later on via
     * {@link #getTreeData()}:
     *
     * <pre>
     * <code>
     * Stream&lt;Person&gt; grandParents = getGrandParents();
     * HasHierarchicalDataProvider&lt;Person&gt; treeGrid = new TreeGrid&lt;&gt;();
     * treeGrid.setItems(grandParents, Person::getChildren);
     * ...
     *
     * TreeData&lt;Person&gt; data = treeGrid.getTreeData();
     * </code>
     * </pre>
     * <p>
     * The returned {@link TreeData} instance may be used as-is to add, remove
     * or modify items in the hierarchy. These modifications to the object are
     * not automatically reflected back to the TreeGrid. Items modified should
     * be refreshed with {@link HierarchicalDataProvider#refreshItem(Object)}
     * and when adding or removing items
     * {@link HierarchicalDataProvider#refreshAll()} should be called.
     *
     * @param rootItems
     *            the root items to display, not {@code null}
     * @param childItemProvider
     *            the value provider used to recursively populate the given root
     *            items with child items, not {@code null}
     */
    public default void setItems(Stream<T> rootItems,
            ValueProvider<T, Stream<T>> childItemProvider) {
        Objects.requireNonNull(rootItems, "Given root items may not be null");
        Objects.requireNonNull(childItemProvider,
                "Given child item provider may not be null");
        setDataProvider(new TreeDataProvider<>(
                new TreeData<T>().addItems(rootItems, childItemProvider)));
    }

    /**
     * Sets the hierarchical data provider for this listing. The data provider
     * provides the items and the hierarchy as needed.
     *
     * @param hierarchicalDataProvider
     *            the hierarchical data provider to use, not {@code null}
     */
    void setDataProvider(
            HierarchicalDataProvider<T, ?> hierarchicalDataProvider);
}
