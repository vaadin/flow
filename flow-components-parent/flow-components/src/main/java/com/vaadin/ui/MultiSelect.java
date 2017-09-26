package com.vaadin.ui;

import java.util.Set;

import com.vaadin.ui.common.HasValue;

/**
 * Multi selection component which allows to select and deselect multiple items.
 *
 * @author Vaadin Ltd
 *
 * @param <C>
 *            the listing component type
 * @param <T>
 *            the type of the items to select
 */
public interface MultiSelect<L extends AbstractListing<T>, T>
        extends HasValue<L, Set<T>> {

}
