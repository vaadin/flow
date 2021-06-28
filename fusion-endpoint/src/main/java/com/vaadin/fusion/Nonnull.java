package com.vaadin.fusion;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to mark either field, method, parameter or collection item type
 * as non-nullable. It is used by Typescript Generator as a source of type
 * nullability information.
 *
 * Since Typescript Generator works with any annotation with case-insensitive
 * `nonnull` name, this annotation exists only for convenience because the most
 * popular `javax.annotation.Nonnull` annotation is not applicable to collection
 * item types.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.TYPE_USE, ElementType.TYPE_PARAMETER })
public @interface Nonnull {
}
