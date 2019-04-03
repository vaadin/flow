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

import java.lang.reflect.Modifier;
import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;

import javax.servlet.ServletContainerInitializer;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.annotation.HandlesTypes;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.WebComponentExporter;
import com.vaadin.flow.server.webcomponent.WebComponentConfigurationRegistry;

/**
 *
 * Servlet initializer for collecting all classes that extend
 * {@link WebComponentExporter} on startup.
 *
 * @author Vaadin Ltd.
 * @since
 */
@HandlesTypes({ WebComponentExporter.class })
public class WebComponentExporterRegistryInitializer
        implements ServletContainerInitializer {

    @Override
    public void onStartup(Set<Class<?>> set, ServletContext servletContext)
            throws ServletException {
        WebComponentConfigurationRegistry instance = WebComponentConfigurationRegistry
                .getInstance(servletContext);

        if (set == null || set.isEmpty()) {
            instance.setExporters(Collections.emptySet());
            return;
        }

        try {
            Set<Class<? extends WebComponentExporter<? extends Component>>> exporterClasses = set
                    .stream().filter(WebComponentExporter.class::isAssignableFrom)
                    .filter(clazz -> !clazz.isInterface()
                            && !Modifier.isAbstract(clazz.getModifiers()))
                    .map(aClass -> (Class<? extends WebComponentExporter<? extends Component>>) aClass)
                    .collect(Collectors.toSet());

//        validateDistinct(exporterClasses);
//
//        Map<String, Class<? extends WebComponentExporter<? extends Component>>> exporterMap = exporterClasses
//                .stream()
//                .collect(Collectors.toMap(
//                        WebComponentExporterRegistryInitializer::getTag,
//                        aClass -> aClass));
//
//        validateComponentName(exporterMap);

            instance.setExporters(exporterClasses);
        }
        catch (Exception e) {
            throw new ServletException(String.format(
                    "%s failed to collect %s implementations!",
                    WebComponentExporterRegistryInitializer.class.getSimpleName(),
                    WebComponentExporter.class.getSimpleName()),
                    e);
        }
    }

    private static String getTag(
            Class<? extends WebComponentExporter<? extends Component>> exporterClass) {
        Tag tag = exporterClass.getAnnotation(Tag.class);
        if (tag == null) {
            throw new IllegalArgumentException(String.format("%s %s did not "
                    + "provide a tag! Use %s annotation to provide a tag for "
                    + "the exported web component.",
                    WebComponentExporter.class.getSimpleName(),
                    exporterClass.getCanonicalName(),
                    Tag.class.getSimpleName()));
        }

        return tag.value();
    }
}
