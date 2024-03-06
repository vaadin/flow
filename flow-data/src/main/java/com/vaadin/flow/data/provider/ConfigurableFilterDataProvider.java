/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.data.provider;

/**
 * A data provider that supports programmatically setting a filter that will be
 * applied to all queries.
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
 */
public interface ConfigurableFilterDataProvider<T, Q, C>
        extends DataProvider<T, Q> {

    /**
     * Sets the filter to use for all queries handled by this data provider.
     *
     * @param filter
     *            the filter to set, or <code>null</code> to clear any
     *            previously set filter
     */
    void setFilter(C filter);

}
