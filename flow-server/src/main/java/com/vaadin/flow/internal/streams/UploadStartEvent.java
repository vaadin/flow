/*
 * Copyright (C) 2000-2026 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.internal.streams;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.ComponentEvent;

/**
 * Event notifying the upload component that the upload has been started.
 * <p>
 * This event is typically used in conjunction with file upload components and
 * {@link com.vaadin.flow.server.streams.UploadHandler} to indicate that the
 * upload process has started. This event is internal and is not intended for
 * public use.
 * <p>
 * For internal use only. May be renamed or removed in a future release.
 *
 * @since 24.8
 */
public class UploadStartEvent extends ComponentEvent<Component> {
    /**
     * Creates a new event using the given source. Always fired on the server
     * side.
     *
     * @param source
     *            the source component
     */
    public UploadStartEvent(Component source) {
        super(source, false);
    }
}
