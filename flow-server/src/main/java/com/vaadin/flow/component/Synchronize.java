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
package com.vaadin.flow.component;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * Annotates getters for which properties should be synchronized to the server.
 * <p>
 * By default deduces the name of the property from the name of the getter
 * unless it has been specified using {@link #property()}.
 *
 * @author Vaadin Ltd
 */
@Documented
@Retention(RUNTIME)
@Target(METHOD)
public @interface Synchronize {

    /**
     * The DOM event(s) to use for synchronization. Those events need to be
     * fired by the root element of the webcomponent (or bubbled to the root
     * element). Events from inner elements of the webcomponent that are not
     * bubbled to the outside are not intercepted by the server.
     *
     * @return the name of the DOM event(s) to use for synchronization
     */
    String[] value();

    /**
     * The name of the property to synchronize. By default deduced from the name
     * of the getter.
     *
     * @return the name of the property to synchronize
     */
    String property() default "";

}
