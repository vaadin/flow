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
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Defines the body size that will be added to the HTML of the host pages.
 * <p>
 * If no {@code @BodySize} has been applied, the default values
 * {@code height:100vh} and {@code width:100%} will be used, so the body will
 * fill the entire viewport. If you don't want to set any size for the body, you
 * must apply an empty {@code @BodySize} annotation to disable the default
 * values.
 *
 * @author Vaadin Ltd
 * @since 1.0
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Inherited
@Documented
public @interface BodySize {

    /**
     * Definition for body height.
     *
     * @return the body height to set
     */
    String height() default "";

    /**
     * Definition for body width.
     *
     * @return the body width to set
     */
    String width() default "";
}
