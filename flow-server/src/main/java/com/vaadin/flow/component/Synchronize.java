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
package com.vaadin.flow.component;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import com.vaadin.flow.dom.DisabledUpdateMode;

/**
 * Annotates getters for which properties should be synchronized to the server.
 * <p>
 * By default deduces the name of the property from the name of the getter
 * unless it has been specified using {@link #property()}.
 *
 * @author Vaadin Ltd
 * @since 1.0
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

    /**
     * Controls updates for the property from the client side to the server side
     * when the element is disabled.
     * <p>
     * When multiple update mode settings are defined for the same property, the
     * most permissive mode is used. This means that there might be unexpected
     * updates for a disabled component if multiple parties independently
     * configure different aspects for the same component. This is based on the
     * assumption that if a property is explicitly safe to update for disabled
     * components in one context, then the nature of that property is probably
     * such that it's also safe to update in other contexts.
     *
     * @return the property update mode for disabled element
     */
    DisabledUpdateMode allowUpdates() default DisabledUpdateMode.ONLY_WHEN_ENABLED;
}
