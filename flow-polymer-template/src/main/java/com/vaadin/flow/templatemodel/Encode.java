/**
 * Copyright (C) 2022 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.templatemodel;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Defines a ModelEncoder on a template model property found through
 * {@link #path()}.
 * <p>
 * Use this annotation on setters in your {@link TemplateModel} class to perform
 * type conversions on properties.
 *
 * @see ModelEncoder
 *
 * @author Vaadin Ltd
 * @since 1.0
 *
 * @deprecated Template model and polymer template support is deprecated - we
 *             recommend you to use {@code LitTemplate} instead. Read more
 *             details from <a href=
 *             "https://vaadin.com/blog/future-of-html-templates-in-vaadin">the
 *             Vaadin blog.</a>
 */
@Deprecated
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
@Repeatable(Encode.Container.class)
public @interface Encode {

    /**
     * The ModelEncoder class to use for encoding the property found through
     * {{@link #path()}.
     *
     * @return the ModelEncoder class
     */
    Class<? extends ModelEncoder<?, ?>> value();

    /**
     * The dot separated path from the TemplateModel property to the value to
     * apply encoding to. Empty string by default, which will apply encoding
     * directly to the property.
     *
     * @return the dot separated path to the bean property to encode, empty
     *         string by default
     */
    String path() default "";

    /**
     * Internal annotation to enable use of multiple {@link Encode} annotations.
     */
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.METHOD)
    @Documented
    public @interface Container {

        /**
         * Internally used to enable use of multiple {@link Encode} annotations.
         *
         * @return an array of the Encode annotations
         */
        Encode[] value();
    }
}
