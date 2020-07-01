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

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.ComponentEvent;

/**
 * Event describing the item count change for a component.
 * The ItemCountChangedEvent will fired from beforeClientResponse so changes
 * done during the server round trip will only receive one event.
 *
 * @param <T> the event source type
 * @since
 */
public class ItemCountChangeEvent<T extends Component> extends ComponentEvent<T> {
    private int itemCount;

    /**
     * Creates a new event using the given source and indicator whether the
     * event originated from the client side or the server side.
     *
     * @param source    the source component
     * @param itemCount new items count
     */
    public ItemCountChangeEvent(T source, int itemCount) {
        super(source, false);
        this.itemCount = itemCount;
    }

    /**
     * Get the new items count for the component data.
     *
     * @return items count
     */
    public int getItemCount() {
        return itemCount;
    }
}
