/*
 * Copyright 2000-2025 Vaadin Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.vaadin.flow.server.frontend.scanner;

import java.util.List;

import org.objectweb.asm.AnnotationVisitor;

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
            cssData.setValue(value);
        } else if (FrontendClassVisitor.ID.equals(name)) {
            cssData.setId(value);
        } else if (FrontendClassVisitor.INCLUDE.equals(name)) {
            cssData.setInclude(value);
        } else if (FrontendClassVisitor.THEME_FOR.equals(name)) {
            cssData.setThemefor(value);
        }
    }

    @Override
    public AnnotationVisitor visitAnnotation(String name, String descriptor) {
        // visited when annotation is repeated in the class
        newData();
        return this;
    }
}
