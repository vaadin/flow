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
package com.vaadin.flow.data.provider;

import java.io.Serializable;

/**
 * Listener interface for getting updates on data item count changes.
 * <p>
 * Items count changes are mostly due to filtering of the data, but can also be
 * sent for changes in the dataset.
 * <p>
 * The {@link #itemCountChanged(ItemCountChangeEvent)} will be called
 * during the "before client response"-phase, so changes done during the
 * server round trip will only receive one event.
 * For example, this code will trigger only one
 * {@link #itemCountChanged(ItemCountChangeEvent)} method call, although there
 * are two methods called which cause the item count change:
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
     *         Component event containing new data size
     */
    void itemCountChanged(ItemCountChangeEvent event);

}
