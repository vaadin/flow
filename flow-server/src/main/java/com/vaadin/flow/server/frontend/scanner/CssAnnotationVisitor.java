package com.vaadin.flow.server.frontend.scanner;

import java.util.HashSet;

import net.bytebuddy.jar.asm.AnnotationVisitor;

import com.vaadin.flow.component.dependency.CssImport;

/**
 * Visitor for {@link CssImport}.
 */
final class CssAnnotationVisitor extends RepeatedAnnotationVisitor {
    private CssData cssData;
    private final HashSet<CssData> cssSet;
    
    CssAnnotationVisitor(HashSet<CssData> cssSet) {
        this.cssSet = cssSet;
    }

    private void newData() {
        cssData = new CssData();
        if (cssSet != null) {
            cssSet.add(cssData);
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
