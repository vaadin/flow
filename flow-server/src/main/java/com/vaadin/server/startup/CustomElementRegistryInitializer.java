/*
 * Copyright 2000-2017 Vaadin Ltd.
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
package com.vaadin.server.startup;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

import javax.servlet.ServletContainerInitializer;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.annotation.HandlesTypes;

import com.vaadin.ui.Component;
import com.vaadin.ui.Tag;

/**
 * Servlet initializer for collecting all applicable custom element tag names on
 * startup.
 */
@HandlesTypes(Tag.class)
public class CustomElementRegistryInitializer
        extends AbstractCustomElementRegistryInitializer
        implements ServletContainerInitializer {

    @Override
    public void onStartup(Set<Class<?>> classSet, ServletContext servletContext)
            throws ServletException {
        CustomElementRegistry elementRegistry = CustomElementRegistry
                .getInstance();

        Map<String, Class<? extends Component>> customElements = Collections
                .emptyMap();
        if (classSet != null) {
            customElements = filterCustomElements(classSet.stream());
        }

        if (!elementRegistry.isInitialized()) {
            elementRegistry.setCustomElements(customElements);
        }
    }

}
