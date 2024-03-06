/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.component;

import java.io.Serializable;

import com.vaadin.flow.dom.Element;

/**
 * Marker interface for any class which is based on an {@link Element}.
 *
 * @author Vaadin Ltd
 * @since 1.0
 */
@FunctionalInterface
public interface HasElement extends Serializable {
    /**
     * Gets the element associated with this instance.
     *
     * @return the element associated with this instance
     */
    Element getElement();

}
