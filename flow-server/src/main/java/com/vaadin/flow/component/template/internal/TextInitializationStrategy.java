/**
 * Copyright (C) 2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See {@literal <https://vaadin.com/commercial-license-and-service-terms>}  for the full
 * license.
 */
package com.vaadin.flow.component.template.internal;

import java.io.Serializable;

import com.vaadin.flow.dom.Element;

/**
 * Initializes Element via setting a text value.
 * <p>
 * For internal use only. May be renamed or removed in a future release.
 *
 * @author Vaadin Ltd
 * @since
 *
 */
class TextInitializationStrategy
        implements ElementInitializationStrategy, Serializable {

    @Override
    public void initialize(Element element, String name, String value) {
        element.setText(value);
    }

}