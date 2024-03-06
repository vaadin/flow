/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.data.provider.hierarchy;

import com.vaadin.flow.data.provider.BackEndDataProvider;

/**
 * A data provider that lazy loads items from a back end containing hierarchical
 * data.
 *
 * @author Vaadin Ltd
 *
 * @param <T>
 *            data provider data type
 * @param <F>
 *            data provider filter type
 * @since 1.2
 */
public interface BackEndHierarchicalDataProvider<T, F>
        extends HierarchicalDataProvider<T, F>, BackEndDataProvider<T, F> {

}
