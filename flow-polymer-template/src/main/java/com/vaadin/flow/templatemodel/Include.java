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
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Defines which properties to include when importing a bean into a template
 * model.
 * <p>
 * Use this annotation on bean setters in your {@link TemplateModel} class to
 * restrict which properties of the beans are imported into the model.
 * <p>
 * You can only define exact matches using this filter. If you need more
 * control, you can use
 * {@link TemplateModel#importBean(String, Object, java.util.function.Predicate)}
 * and define a custom filter.
 * <p>
 * Note that <code>@Include</code> annotations are not inherited.
 *
 * @see Exclude
 *
 * @since 1.0
 *
 * @deprecated Template model and polymer template support is deprecated - we
 *             recommend you to use {@code LitTemplate} instead. Read more
 *             details from <a href=
 *             "https://vaadin.com/blog/future-of-html-templates-in-vaadin">the
 *             Vaadin blog.</a> For lit templates, you can use {@code @Id}
 *             mapping and the component API or the element API with property
 *             synchronization instead.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
@Documented
public @interface Include {

    /**
     * Properties to include from a bean when importing into a template model.
     * <p>
     * By default all properties are included.
     *
     * @return the properties to include from a bean when importing into a
     *         template model
     */
    String[] value();
}
