/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.component.page;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Defines a meta tag with customized name and content that will be added to the
 * HTML of the host page of a UI class.
 *
 * @author Vaadin Ltd
 * @since 1.1
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Inherited
@Documented
@Repeatable(Meta.Container.class)
public @interface Meta {
    /**
     * Gets the custom tag name.
     *
     * @return the custom tag name
     */
    String name();

    /**
     * Gets the custom tag content.
     *
     * @return the custom tag content
     */
    String content();

    /**
     * Internal annotation to enable use of multiple {@link Meta} annotations.
     */
    @Target(ElementType.TYPE)
    @Retention(RetentionPolicy.RUNTIME)
    @Inherited
    @Documented
    public @interface Container {
        /**
         * Internally used to enable use of multiple {@link Meta} annotations.
         *
         * @return an array of the Meta annotations
         */
        Meta[] value();
    }
}
