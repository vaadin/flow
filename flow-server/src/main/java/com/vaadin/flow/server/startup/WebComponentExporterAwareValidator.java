/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.server.startup;

import javax.servlet.annotation.HandlesTypes;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import com.googlecode.gentyref.GenericTypeReflector;

import com.vaadin.flow.component.WebComponentExporter;
import com.vaadin.flow.component.page.Push;
import com.vaadin.flow.server.VaadinContext;

/**
 * Checks that specific annotations are not configured wrong.
 * <p>
 * The validation is run during servlet container initialization.
 * <p>
 * For internal use only. May be renamed or removed in a future release.
 *
 * @author Vaadin Ltd.
 * @since 2.0
 */
@HandlesTypes(Push.class)
public class WebComponentExporterAwareValidator
        extends AbstractAnnotationValidator
        implements VaadinServletContextStartupInitializer {

    @Override
    public void initialize(Set<Class<?>> classSet, VaadinContext context) {
        validateClasses(AbstractAnnotationValidator
                .removeHandleTypesSelfReferences(classSet, this));
    }

    @Override
    protected Optional<String> handleNonRouterLayout(Class<?> clazz) {
        if (WebComponentExporter.class
                .isAssignableFrom(GenericTypeReflector.erase(clazz))) {
            return Optional.empty();
        }
        return Optional.of(String.format(
                "Class '%s' contains '%s', but it is not a router "
                        + "layout/top level route/web component.",
                clazz.getName(), getClassAnnotations(clazz)));
    }

    @Override
    protected String getErrorHint() {
        return "Found configuration annotations"
                + " that will not be used in the application. \n"
                + "Move it to a single route/a top router layout/web component of the application. \n";
    }

    @Override
    public List<Class<?>> getAnnotations() {
        return Arrays.asList(
                this.getClass().getAnnotation(HandlesTypes.class).value());
    }

}
