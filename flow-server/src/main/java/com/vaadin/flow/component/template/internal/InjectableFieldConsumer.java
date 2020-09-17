package com.vaadin.flow.component.template.internal;

import java.lang.reflect.Field;

/**
 * Three argument consumer.
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