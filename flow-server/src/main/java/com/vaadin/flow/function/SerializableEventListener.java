/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.function;

import java.io.Serializable;
import java.util.EventListener;

/**
 * Event listener interface that is serializable.
 *
 * @author Vaadin Ltd
 * @since 1.0
 */
public interface SerializableEventListener extends EventListener, Serializable {

}
