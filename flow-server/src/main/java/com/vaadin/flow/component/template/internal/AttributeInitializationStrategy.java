/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.component.template.internal;

import com.vaadin.flow.dom.Element;

/**
 * Initializes Element via setting an attribute.
 * <p>
 * For internal use only. May be renamed or removed in a future release.
 *
 * @author Vaadin Ltd
 * @since
 *
 */
class AttributeInitializationStrategy implements ElementInitializationStrategy {

    @Override
    public void initialize(Element element, String name, String value) {
        element.setAttribute(name, value);
    }

}
