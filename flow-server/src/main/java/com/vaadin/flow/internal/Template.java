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

/**
 * Marker interface for (Lit and Polymer) templates. All frontend files linked
 * by implementors (with {@link com.vaadin.flow.component.dependency.JsModule})
 * will be copied to {@code META-INF/VAADIN/config/templates}.
 *
 * @author Vaadin Ltd
 */
public interface Template extends Serializable {
}
