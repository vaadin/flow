/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.internal;

import java.io.Serializable;

import com.vaadin.flow.component.UI;

/**
 * Context of a callback execution when
 * {@link UI#beforeClientResponse(com.vaadin.flow.component.Component, com.vaadin.flow.function.SerializableConsumer)}
 * is invoked.
 * <p>
 * For internal use only. May be renamed or removed in a future release.
 *
 * @author Vaadin Ltd
 * @since 1.0.
 *
 */
public class ExecutionContext implements Serializable {

    private final UI ui;
    private final boolean clientSideInitialized;

    /**
     * Creates a new, immutable context.
     *
     * @param ui
     *            The UI associated with the execution
     * @param clientSideInitialized
     *            <code>true</code> if the client side is already initialized,
     *            <code>false</code> if it is being initialized as part of the
     *            current response
     */
    public ExecutionContext(UI ui, boolean clientSideInitialized) {
        assert ui != null;
        this.ui = ui;
        this.clientSideInitialized = clientSideInitialized;
    }

    /**
     * Gets the UI associated with the execution.
     *
     * @return the UI, not <code>null</code>
     */
    public UI getUI() {
        return ui;
    }

    /**
     * Gets whether the client side is being initialized as part of the server
     * response.
     *
     * @return <code>true</code> if the client side is already initialized,
     *         <code>false</code> if it is being initialized as part of the
     *         current response
     */
    public boolean isClientSideInitialized() {
        return clientSideInitialized;
    }

}
