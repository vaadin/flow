/**
 * Copyright (C) 2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See {@literal <https://vaadin.com/commercial-license-and-service-terms>}  for the full
 * license.
 */
package com.vaadin.flow.server.startup;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.annotation.HandlesTypes;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

import com.vaadin.flow.component.page.BodySize;
import com.vaadin.flow.component.page.Inline;
import com.vaadin.flow.component.page.Viewport;

/**
 * Validation class that is run during servlet container initialization which
 * checks that specific annotations are not configured wrong.
 * <p>
 * For internal use only. May be renamed or removed in a future release.
 *
 * @since 1.0
 */
@HandlesTypes({ Viewport.class, BodySize.class, Inline.class })
public class AnnotationValidator extends AbstractAnnotationValidator
        implements ClassLoaderAwareServletContainerInitializer {

    @Override
    public void process(Set<Class<?>> classSet, ServletContext servletContext)
            throws ServletException {
        validateClasses(classSet);
    }

    @Override
    public List<Class<?>> getAnnotations() {
        return Arrays.asList(
                this.getClass().getAnnotation(HandlesTypes.class).value());
    }

}
