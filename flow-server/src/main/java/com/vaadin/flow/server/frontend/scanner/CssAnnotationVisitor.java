/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.server.frontend.scanner;

import java.util.List;

import net.bytebuddy.jar.asm.AnnotationVisitor;

import com.vaadin.flow.component.dependency.CssImport;

/**
 * Visitor for {@link CssImport}.
 * <p>
 * For internal use only. May be renamed or removed in a future release.
 *
 * @since 2.0
 */
final class CssAnnotationVisitor extends RepeatedAnnotationVisitor {
    private CssData cssData;
    private final List<CssData> cssList;

    /**
     * This visitor needs a list to be updated.
     *
     * @param cssList
     *            the list to update with this annotation values
     */
    CssAnnotationVisitor(List<CssData> cssList) {
        this.cssList = cssList;
    }

    private void newData() {
        cssData = new CssData();
        if (cssList != null) {
            cssList.add(cssData);
        }
    }

    @Override
    public void visit(String name, Object obj) {
        String value = String.valueOf(obj);
        if (cssData == null) {
            // visited when only one annotation in the class
            newData();
        }
        if (FrontendClassVisitor.VALUE.equals(name)) {
            cssData.value = value;
        } else if (FrontendClassVisitor.ID.equals(name)) {
            cssData.id = value;
        } else if (FrontendClassVisitor.INCLUDE.equals(name)) {
            cssData.include = value;
        } else if (FrontendClassVisitor.THEME_FOR.equals(name)) {
            cssData.themefor = value;
        }
    }

    @Override
    public AnnotationVisitor visitAnnotation(String name, String descriptor) {
        // visited when annotation is repeated in the class
        newData();
        return this;
    }
}
