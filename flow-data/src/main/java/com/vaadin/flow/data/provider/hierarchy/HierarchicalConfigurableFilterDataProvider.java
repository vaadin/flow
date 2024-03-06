/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.data.provider.hierarchy;

import com.vaadin.flow.data.provider.ConfigurableFilterDataProvider;

/**
 * A hierarchical data provider that supports programmatically setting a filter
 * that will be applied to all queries.
 *
 * @author Vaadin Ltd
 * @since 1.0
 *
 * @param <T>
 *            the data provider item type
 * @param <Q>
 *            the query filter type
 * @param <C>
 *            the configurable filter type
 *
 * @see ConfigurableFilterDataProvider
 */
public interface HierarchicalConfigurableFilterDataProvider<T, Q, C>
        extends ConfigurableFilterDataProvider<T, Q, C>,
        HierarchicalDataProvider<T, Q> {

}
