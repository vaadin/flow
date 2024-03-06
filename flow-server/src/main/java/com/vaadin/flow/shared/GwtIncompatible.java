/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.shared;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a type, method, constructor of field to be ignored by the GWT compiler.
 * We have our own copy of the annotation to avoid depending on
 * <code>gwt-shared</code>. See the documentation for
 * <code>com.google.gwt.core.shared.GwtIncompatible</code> for more information.
 *
 * @since
 */
@Retention(RetentionPolicy.SOURCE)
@Target({ ElementType.TYPE, ElementType.METHOD, ElementType.CONSTRUCTOR,
        ElementType.FIELD })
public @interface GwtIncompatible {
    /**
     * Has no technical meaning, is only used for documentation
     *
     * @return a description of the incompatibility reason
     */
    String value();
}
