package com.vaadin.flow.component.template.internal;

import com.vaadin.flow.dom.Element;

/**
 * Initializes Element via setting an attribute.
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
