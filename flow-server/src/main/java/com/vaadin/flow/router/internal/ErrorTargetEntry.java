/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.router.internal;

import java.io.Serializable;

import com.vaadin.flow.component.Component;

/**
 * A pair of a navigation target for handling exceptions and the exception type
 * handled by the navigation target.
 * <p>
 * For internal use only. May be renamed or removed in a future release.
 *
 * @since 1.3
 */
public class ErrorTargetEntry implements Serializable {
    private final Class<? extends Component> navigationTarget;
    private final Class<? extends Exception> handledExceptionType;

    /**
     * Creates a new new entry with the given navigation target type and
     * exception type.
     *
     * @param navigationTarget
     *            the navigation target type, not <code>null</code>
     * @param handledExceptionType
     *            the exception type handled by the navigation target, not
     *            <code>null</code>
     */
    public ErrorTargetEntry(Class<? extends Component> navigationTarget,
            Class<? extends Exception> handledExceptionType) {
        assert navigationTarget != null;
        assert handledExceptionType != null;

        this.navigationTarget = navigationTarget;
        this.handledExceptionType = handledExceptionType;
    }

    /**
     * Gets the navigation target type.
     *
     * @return the navigation target type, not <code>null</code>
     */
    public Class<? extends Component> getNavigationTarget() {
        return navigationTarget;
    }

    /**
     * Gets the exception type handled by the navigation target.
     *
     * @return the exception type, not <code>null</code>
     */
    public Class<? extends Exception> getHandledExceptionType() {
        return handledExceptionType;
    }
}
