/**
 * Copyright (C) 2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See {@literal <https://vaadin.com/commercial-license-and-service-terms>}  for the full
 * license.
 */
package com.vaadin.flow.templatemodel;

import com.vaadin.flow.internal.StateNode;

/**
 * A complex model type (represents either a list or a bean).
 * <p>
 * For internal use only. May be renamed or removed in a future release.
 *
 * @author Vaadin Ltd
 * @since 1.0
 *
 * @param <T>
 *            the proxy type used by this type
 */
public interface ComplexModelType<T> extends ModelType {

    @Override
    StateNode applicationToModel(Object applicationValue,
            PropertyFilter filter);

    /**
     * Checks that this type uses the provided proxy type and returns this type
     * as a model type with that proxy type.
     *
     * @param <C>
     *            the proxy type
     * @param proxyType
     *            the proxy type to cast to
     * @return this model type
     */
    <C> ComplexModelType<C> cast(Class<C> proxyType);
}
