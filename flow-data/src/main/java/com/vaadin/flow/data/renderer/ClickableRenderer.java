/**
 * Copyright (C) 2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See {@literal <https://vaadin.com/commercial-license-and-service-terms>}  for the full
 * license.
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
