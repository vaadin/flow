/*
 * Copyright 2000-2018 Vaadin Ltd.
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

import javax.servlet.ServletContainerInitializer;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.annotation.HandlesTypes;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import com.googlecode.gentyref.GenericTypeReflector;

import com.vaadin.flow.component.WebComponentExporter;
import com.vaadin.flow.component.page.Push;
import com.vaadin.flow.theme.Theme;

/**
 * Checks that specific annotations are not configured wrong.
 * <p>
 * The validation is run during servlet container initialization.
 *
 * @author Vaadin Ltd.
 * @since 2.0
 */
@HandlesTypes({Theme.class, Push.class})
public class WebComponentExporterAwareValidator extends
        AbstractAnnotationValidator implements ServletContainerInitializer {

    @Override
    public void onStartup(Set<Class<?>> classSet, ServletContext servletContext)
            throws ServletException {
        validateClasses(classSet);
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
