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

/**
 * An in-memory data provider for listing components that display hierarchical
 * data. Uses an instance of {@link TreeData} as its source of data.
 *
 * @author Vaadin Ltd
 * @since 1.2
 *
 * @param <T>
 *            data type
 */
public class TreeDataProvider<T> extends HierarchicalTreeDataProvider<T> {

    /**
     * Constructs a new TreeDataProvider.
     * <p>
     * The data provider should be refreshed after making changes to the
     * underlying {@link TreeData} instance.
     *
     * @param treeData
     *            the backing {@link TreeData} for this provider, not
     *            {@code null}
     */
    public TreeDataProvider(TreeData<T> treeData) {
        super(treeData);
    }

    /**
     * Creates a new TreeDataProvider and configures it to return the
     * hierarchical data in the specified format: {@link HierarchyFormat#NESTED}
     * or {@link HierarchyFormat#FLATTENED}.
     * <p>
     * The data provider should be refreshed after making changes to the
     * underlying {@link TreeData} instance.
     *
     * @param treeData
     *            the backing {@link TreeData} for this provider, not
     *            {@code null}
     * @param hierarchyFormat
     *            the hierarchy format to return data in, not {@code null}
     */
    public TreeDataProvider(TreeData<T> treeData,
            HierarchyFormat hierarchyFormat) {
        super(treeData, hierarchyFormat);
    }

    @Override
    public TreeData<T> getTreeData() {
        return (TreeData<T>) super.getTreeData();
    }
}
