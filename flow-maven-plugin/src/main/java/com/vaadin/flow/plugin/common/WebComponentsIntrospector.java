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

import java.net.URL;

/**
 * @author Vaadin Ltd
 *
 */
public class WebComponentsIntrospector extends ClassPathIntrospector {

    /**
     * Prepares the class to find web component exporters from the project
     * classes specified.
     *
     * @param projectClassesLocations
     *            urls to project class locations (directories, jars etc.)
     */
    public WebComponentsIntrospector(URL... projectClassesLocations) {
        super(projectClassesLocations);
    }
}
