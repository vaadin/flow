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
package com.vaadin.flow.data.renderer;

import java.io.Serializable;
import java.util.List;

import com.vaadin.flow.shared.Registration;

/**
 * Represents a clickable renderer.
 *
 * @author Vaadin Ltd
 * @since 1.0.
 *
 * @param <SOURCE>
 *            the type of the item received in the click listeners
 */
public interface ClickableRenderer<SOURCE> extends Serializable {

    /**
     * Listener that receives the clicked item (or tapped item, in touch
     * devices).
     *
     * @param <SOURCE>
     *            the type of the clicked item
     */
    @FunctionalInterface
    public interface ItemClickListener<SOURCE> extends Serializable {

        /**
         * Notifies when an item is clicked or tapped in the target component.
         *
         * @param item
         *            the clicked or tapped item
         */
        void onItemClicked(SOURCE item);
    }

    /**
     * Adds a click listener to the renderer. Events are fired when items are
     * clicked or tapped (for touch devices).
     *
     * @param listener
     *            the listener to receive click events, not <code>null</code>
     * @return a registration that can be used to remove the listener from this
     *         renderer
     */
    Registration addItemClickListener(ItemClickListener<SOURCE> listener);

    /**
     * Gets all registered listeners.
     *
     * @return an unmodifiable list of registered listeners, not
     *         <code>null</code>
     */
    List<ItemClickListener<SOURCE>> getItemClickListeners();

    /**
     * Invoked when an item is clicked or tapped. Registered listeners are
     * notified.
     *
     * @param item
     *            the clicked or tapped item
     * @see #addItemClickListener(ItemClickListener)
     */
    default void onClick(SOURCE item) {
        List<ItemClickListener<SOURCE>> itemClickListeners = getItemClickListeners();
        if (itemClickListeners != null) {
            itemClickListeners
                    .forEach(listener -> listener.onItemClicked(item));
        }
    };
}
