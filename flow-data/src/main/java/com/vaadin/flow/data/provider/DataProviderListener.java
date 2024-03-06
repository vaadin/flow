/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.data.provider;

import java.io.Serializable;

/**
 * Data change events listener.
 *
 * @author Vaadin Ltd
 * @since 1.0
 *
 * @param <T>
 *            the data type
 * @see DataProvider
 */
@FunctionalInterface
public interface DataProviderListener<T> extends Serializable {

    /**
     * Invoked when this listener receives a data change event from a data
     * source to which it has been added.
     * <p>
     * This event is fired when something has changed in the underlying data. It
     * doesn't allow to distinguish different kind of events
     * (add/remove/update). It means that the method implementation normally
     * just reloads the whole data to refresh.
     *
     * @param event
     *            the received event, not null
     */
    void onDataChange(DataChangeEvent<T> event);
}
