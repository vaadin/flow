package com.vaadin.annotations;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Don't use! Use multiple {@link JavaScript @JavaScript} annotations instead.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Inherited
@Documented
public @interface JavaScripts {

    /**
     * Don't use! Use multiple {@link JavaScript @JavaScript} annotations
     * instead.
     *
     * @return value
     */
    JavaScript[] value();
}
