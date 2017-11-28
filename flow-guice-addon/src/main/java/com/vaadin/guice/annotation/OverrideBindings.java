package com.vaadin.guice.annotation;

import com.google.inject.Module;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;


/**
 * Annotation to be placed on {@link com.google.inject.Module}-classes if they are to 'overwrite'
 * bindings from other modules.
 *
 * @author Bernd Hopp (bernd@vaadin.com)
 * @see com.google.inject.util.Modules#override(Module...) <p>
 * <pre>
 * &#064;OverrideBindings
 * public class OverwritingModule extends AbstractModule {
 * }
 * </pre>
 */
@Target({ElementType.TYPE})
@Retention(java.lang.annotation.RetentionPolicy.RUNTIME)
@Documented
public @interface OverrideBindings {
}
