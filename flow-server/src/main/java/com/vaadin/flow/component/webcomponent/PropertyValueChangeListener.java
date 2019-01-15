package com.vaadin.flow.component.webcomponent;

import java.io.Serializable;

/**
 *
 */
@FunctionalInterface
public interface PropertyValueChangeListener extends Serializable {

    /**
     * Method called when target property value has changed.
     *
     * @param event
     *         property value change event
     */
    void valueChange(PropertyValueChangeEvent event);
}