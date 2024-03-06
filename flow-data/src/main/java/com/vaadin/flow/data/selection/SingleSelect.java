/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.data.selection;

import com.vaadin.flow.component.AbstractField.ComponentValueChangeEvent;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.HasValueAndElement;

/**
 * Single selection component whose selection is treated as a value.
 *
 * @author Vaadin Ltd
 * @since 1.0
 *
 * @param <C>
 *            the selection component type
 * @param <T>
 *            the selection value type
 *
 */
public interface SingleSelect<C extends Component, T>
        extends HasValueAndElement<ComponentValueChangeEvent<C, T>, T> {

}
