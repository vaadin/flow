/**
 * Copyright (C) 2022 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.templatemodel;

/**
 * A mode for whether a model property may be updated from the client.
 *
 * @see AllowClientUpdates
 *
 * @author Vaadin Ltd
 * @since 1.0
 * @deprecated Template model and polymer template support is deprecated - we
 *             recommend you to use {@code LitTemplate} instead. Read more
 *             details from <a href=
 *             "https://vaadin.com/blog/future-of-html-templates-in-vaadin">the
 *             Vaadin blog.</a>
 */
@Deprecated
public enum ClientUpdateMode {
    /**
     * Always allow updating the property.
     */
    ALLOW,
    /**
     * Never allow updating the property.
     */
    DENY,
    /**
     * Allow updating the property if there is a corresponding two-way binding
     * in the template. This is the default mode that is used if nothing else
     * has been defined for a property.
     */
    IF_TWO_WAY_BINDING;
}
