/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.experimental;

/**
 * Exception thrown for when a FeatureFlag that doesn't exist is checked.
 * <p>
 * For internal use only. May be renamed or removed in a future release.
 */
public class UnknownFeatureException extends RuntimeException {

    /**
     * Exception constructor.
     *
     * @param feature
     *            feature title for feature that was used
     */
    public UnknownFeatureException(String feature) {
        super("Unknown feature used " + feature);
    }
}
