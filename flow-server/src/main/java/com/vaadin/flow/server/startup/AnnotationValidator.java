/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.server.startup;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import javax.servlet.annotation.HandlesTypes;

import com.vaadin.flow.component.page.BodySize;
import com.vaadin.flow.component.page.Inline;
import com.vaadin.flow.component.page.Viewport;
import com.vaadin.flow.server.VaadinContext;

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
        implements VaadinServletContextStartupInitializer {

    @Override
    public void initialize(Set<Class<?>> classes, VaadinContext context) {
        validateClasses(removeHandleTypesSelfReferences(classes, this));
    }

    @Override
    public List<Class<?>> getAnnotations() {
        return Arrays.asList(
                this.getClass().getAnnotation(HandlesTypes.class).value());
    }

}
