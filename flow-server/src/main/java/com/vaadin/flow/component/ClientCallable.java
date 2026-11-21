/*
 * Copyright 2000-2026 Vaadin Ltd.
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

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import com.vaadin.flow.dom.DisabledUpdateMode;

/**
 * Publishes the annotated method so it can be invoked from the client side
 * using the notation <code>this.$server.method()</code>. The method will return
 * a Promise which will be resolved with either the return value from the server
 * or a generic rejection if the server-side method throws an exception.
 * <p>
 * Annotated methods are also published from superclasses, and from default
 * methods of implemented interfaces. An annotated abstract interface method is
 * not published: annotate the method that implements it instead.
 *
 * @author Vaadin Ltd
 * @since 1.0
 *
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
@Documented
public @interface ClientCallable {

    /**
     * Controls RPC for the method from the client side to the server side when
     * the element is disabled.
     * <p>
     * If the method overrides another annotated method, the value of the most
     * specific declaration is used.
     *
     * @return the property update mode for disabled element
     */
    DisabledUpdateMode value() default DisabledUpdateMode.ONLY_WHEN_ENABLED;
}
