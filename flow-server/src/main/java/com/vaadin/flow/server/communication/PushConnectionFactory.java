/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.server.communication;

import java.util.ServiceLoader;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.function.SerializableFunction;

/**
 * Factory that produces instances of {@link PushConnection}.
 *
 * Produces a {@link PushConnection} for the provided {@link UI}.
 *
 * Factory instances are by default discovered and instantiated using
 * {@link ServiceLoader}. This means that all implementations must have a
 * zero-argument constructor and the fully qualified name of the implementation
 * class must be listed on a separate line in a
 * META-INF/services/com.vaadin.flow.server.communication.PushConnectionFactory
 * file present in the jar file containing the implementation class.
 * <p>
 * For internal use only. May be renamed or removed in a future release.
 *
 * @since 1.0
 */
public interface PushConnectionFactory
        extends SerializableFunction<UI, PushConnection> {
}
