/*
 * Copyright 2000-2020 Vaadin Ltd.
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
package com.vaadin.flow.component.template.internal;

import com.vaadin.flow.dom.Element;

/**
 * Defines the strategy to set the template attribute value to the server side
 * element.
 * 
 * @author Vaadin Ltd
 * @since
 *
 */
@FunctionalInterface
interface ElementInitializationStrategy {

    /**
     * Initializes the {@code element} with template attribute {@code name} and
     * its {@code value}.
     * 
     * @param element
     *            the element to initialize
     * @param name
     *            the template attribute name
     * @param value
     *            the attribute value
     */
    void initialize(Element element, String name, String value);
}
