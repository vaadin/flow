package com.vaadin.annotations;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import com.vaadin.ui.Component;
import com.vaadin.ui.UI;

/**
 * Annotation for defining a style sheet dependency on a {@link Component}
 * class.
 * <p>
 * It is guaranteed that the style sheet files are loaded on the client side
 * before the component is used for the first time in a {@link UI}.
 *
 * @author Vaadin Ltd
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Inherited
@Documented
@Repeatable(StyleSheets.class)
public @interface StyleSheet {

    /**
     * Style sheet file URL to load before using the annotated {@link Component}
     * in the browser.
     * <p>
     * Relative URLs are interpreted as relative to the service (servlet) path.
     * You can prefix the URL with {@literal context://} to make it relative to
     * the context path or use an absolute URL to refer to files outside the
     * service (servlet) path.
     *
     * @return a style sheet file URL, not <code>null</code>
     */
    String value();
}
