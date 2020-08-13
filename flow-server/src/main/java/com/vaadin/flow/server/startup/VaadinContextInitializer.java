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
package com.vaadin.flow.server.startup;

import com.vaadin.flow.server.VaadinContext;

import java.io.Serializable;
import java.util.Set;

/**
 * Provides a mechanism to load initializers via {@link VaadinContext}.
 *
 * @since
 */
public interface VaadinContextInitializer extends Serializable {

    /**
     * Implement this method to handle initializer using {@link VaadinContext}.
     *
     * @param set
     *            the Set of application classes that extend, implement, or have
     *            been annotated with the class types specified by the
     *            {@link javax.servlet.annotation.HandlesTypes}
     *            annotation, or <tt>null</tt> if there are no matches, or this
     *            initializer has not been annotated with <tt>HandlesTypes</tt>
     *
     * @param ctx
     *            the {@link VaadinContext} to use with initializer
     */
    void process(Set<Class<?>> set, VaadinContext ctx);

}
