package com.vaadin.flow.router.internal;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks which classes should trigger loading of a chunk defined by a route.
 * <p>
 * This only exists only for internal use.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Inherited
@Documented
public @interface DependencyTrigger {
    /**
     * The classes the should trigger loading of dependencies.
     *
     * @return the trigger classes
     */
    Class<?>[] value();
}
