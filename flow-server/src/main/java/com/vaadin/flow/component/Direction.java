/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.component;

/**
 * Specifies the direction of the text and other content inside of an element.
 *
 * @author Vaadin Ltd
 * @since 3.1
 */
public enum Direction {

    RIGHT_TO_LEFT("rtl"), LEFT_TO_RIGHT("ltr");

    private final String clientName;

    Direction(String clientName) {
        this.clientName = clientName;
    }

    /**
     * Gets the value applied as the {@code dir} attribute in html for
     * {@code document}.
     *
     * @return the value applied as the "dir" attribute.
     */
    public String getClientName() {
        return clientName;
    }

}
