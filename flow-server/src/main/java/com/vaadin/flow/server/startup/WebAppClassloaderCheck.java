/*
 * Copyright 2000-2021 Vaadin Ltd.
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

import javax.servlet.ServletContext;

import com.vaadin.flow.server.VaadinContext;

/**
 * Checks whether the web application {@link ClassLoader} is a parent of the
 * class' classloader.
 * 
 * @author Vaadin Ltd
 * @since
 *
 */
class WebAppClassloaderCheck {

    private final ServletContext context;

    /**
     * Create a new instance of the class using {@code VaadinContext}.
     * 
     * @param context
     *            a {@link VaadinContext} to get the web application classloader
     */
    WebAppClassloaderCheck(ServletContext context) {
        this.context = context;
    }

    boolean hasParentWebClassloader() {
        ClassLoader webClassLoader = context.getClassLoader();
        ClassLoader classLoader = getClass().getClassLoader();

        boolean hasParentWebClassloader = false;
        while (classLoader != null) {
            if (classLoader.equals(webClassLoader)) {
                hasParentWebClassloader = true;
                break;
            } else {
                classLoader = classLoader.getParent();
            }
        }
        return hasParentWebClassloader;
    }

}
