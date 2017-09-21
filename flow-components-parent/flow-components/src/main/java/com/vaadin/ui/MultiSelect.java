package com.vaadin.ui;

import com.vaadin.ui.common.HasValue;

/**
 * Multi selection component which allows to select and deselect multiple items.
 *
 * @author Vaadin Ltd
 *
 * @param <C>
 *            the component type
 * @param <T>
 *            the type of the items to select
 */
public interface MultiSelect<C extends Component, T> extends HasValue<C, T> {

}
