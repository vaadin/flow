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
package com.vaadin.flow.server.startup;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

import jakarta.servlet.annotation.HandlesTypes;

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
