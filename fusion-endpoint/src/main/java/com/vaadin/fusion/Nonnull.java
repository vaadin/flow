/**
 * Copyright (C) 2000-2023 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.fusion;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to mark either field, method, parameter or type parameter as
 * non-nullable. It is used by Typescript Generator as a source of type
 * nullability information.
 *
 * Since Typescript Generator works with any annotation with case-insensitive
 * `nonnull` name, this annotation exists only for convenience because the
 * traditional `javax.annotation.Nonnull` annotation is not applicable to type
 * parameters.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.TYPE_USE })
public @interface Nonnull {
}
