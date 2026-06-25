/*
 * Copyright (C) 2000-2026 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.component;

/**
 * A component which the children components are ordered, so the index of each
 * child matters for the layout.
 * <p>
 * Note: The default methods have been moved to {@link HasComponents}, so that
 * they are available for all components, not just those implementing
 * {@link HasOrderedComponents}. This interface is left for backward
 * compatibility, but it is not needed anymore.
 *
 * @since 1.0
 * @deprecated since 24.10.0, for removal in 26.0.0.
 */
@Deprecated(since = "24.10.0", forRemoval = true)
public interface HasOrderedComponents extends HasComponents {

}
