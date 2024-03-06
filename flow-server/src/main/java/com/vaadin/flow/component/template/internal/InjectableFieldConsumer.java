/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.component.template.internal;

import java.lang.reflect.Field;

/**
 * Three argument consumer.
 * <p>
 * For internal use only. May be renamed or removed in a future release.
 *
 * @author Vaadin Ltd
 * @since
 *
 */
@FunctionalInterface
public interface InjectableFieldConsumer {

    /**
     * Performs this operation on the given arguments.
     * <p>
     * The arguments are: the field declared in a template class, the identifier
     * of the element inside the HTML template file, the element tag.
     *
     * @param field
     *            the field declared in a template class
     * @param id
     *            the element id
     * @param tag
     *            the element tag
     */
    void apply(Field field, String id, String tag);
}
