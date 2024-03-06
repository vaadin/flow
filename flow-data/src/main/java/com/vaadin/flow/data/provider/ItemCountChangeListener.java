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
 * Listener interface for getting updates on data item count changes.
 * <p>
 * Items count changes are mostly due to filtering of the data, but can also be
 * sent for changes in the dataset.
 * <p>
 * The {@link #itemCountChanged(ItemCountChangeEvent)} will be called during the
 * "before client response"-phase, so changes done during the server round trip
 * will only receive one event. For example, this code will trigger only one
 * {@link #itemCountChanged(ItemCountChangeEvent)} method call, although there
 * are two methods called which cause the item count change:
 *
 * <pre>
 * {@code
 * dataView.addItemCountChangeListener(listener);
 * dataView.addItem(newItem);
 * dataView.setFilter(filter);
 * }
 * </pre>
 *
 * @since
 */
@FunctionalInterface
public interface ItemCountChangeListener extends Serializable {

    /**
     * Invoked for changes in the data size.
     *
     * @param event
     *            Component event containing new data size
     */
    void itemCountChanged(ItemCountChangeEvent event);

}
