package com.vaadin.guice.annotation;

import com.vaadin.ui.Component;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * Classes annotated with controller will be created with every UI and in the Scope of the created
 * UI, regardless whether they get injected somewhere or not. This is useful for
 * 'controller'-classes in the MVC-Pattern that typically are not part of the injection-graph but
 * need to be instantiated with a UI.
 *
 * @author Bernd Hopp (bernd@vaadin.com)
 */
@Target({java.lang.annotation.ElementType.TYPE})
@Retention(java.lang.annotation.RetentionPolicy.RUNTIME)
@Documented
public @interface Controller {

    /**
     * the Component that's to be bound to the annotated controller. It is guaranteed that {@link
     * com.google.inject.Injector#getInstance(Class)} is called for the annotated class after the
     * component in 'value' was created. Depending on the {@link com.google.inject.Scope} of the
     * annotation, it is not guaranteed that a new instance is created. However, the existence of at
     * least one instance of the annotated class is guaranteed after an instance of the class of
     * 'value' was created.
     */
    Class<? extends Component> value();
}