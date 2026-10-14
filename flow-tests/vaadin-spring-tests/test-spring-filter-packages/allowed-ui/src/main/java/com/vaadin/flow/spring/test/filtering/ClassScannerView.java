/*
 * Copyright (C) 2000-2026 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.spring.test.filtering;

import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.router.Route;

@Route("")
public class ClassScannerView extends Div {

    public static Set<Class<?>> classes = Collections.emptySet();
    public static final String SCANNED_CLASSES = "scanned-classes";

    public ClassScannerView() {
        Span scannedClasses = new Span(classes.stream()
                .map(Class::getSimpleName).collect(Collectors.joining(",")));
        scannedClasses.setId(SCANNED_CLASSES);
        add(scannedClasses);
    }

}
