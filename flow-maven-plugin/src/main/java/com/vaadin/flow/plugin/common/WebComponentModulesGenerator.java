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
package com.vaadin.flow.plugin.common;

import java.io.File;
import java.util.Set;
import java.util.stream.Collectors;

import com.vaadin.flow.component.WebComponentExporter;
import com.vaadin.flow.server.webcomponent.WebComponentModulesWriter;

/**
 * @author Vaadin Ltd
 * @since
 */
public class WebComponentModulesGenerator extends ClassPathIntrospector {
    private final Class<?> writerClass;

    public WebComponentModulesGenerator(ClassPathIntrospector introspector) {
        super(introspector);
        writerClass = loadClassInProjectClassLoader(
                WebComponentModulesWriter.class.getName());
    }


    public Set<File> generateWebComponentModules(File outputDirectory) {
        Set<Class<?>> exporterClasses =
                getSubtypes(WebComponentExporter.class).collect(Collectors.toSet());

        return WebComponentModulesWriter.ReflectionUsage.reflectiveWriteWebComponentsToDirectory(
                    writerClass, exporterClasses, outputDirectory, true);
    }
}
