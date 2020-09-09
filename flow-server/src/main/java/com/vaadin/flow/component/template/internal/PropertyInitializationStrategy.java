package com.vaadin.flow.component.template.internal;

import com.vaadin.flow.dom.Element;

/**
 * Initializes Element via setting a property.
 * 
 * @author Vaadin Ltd
 * @since
 *
 */
class PropertyInitializationStrategy implements ElementInitializationStrategy {

    @Override
    public void initialize(Element element, String name, String value) {
        element.setProperty(name, value);
    }

}
