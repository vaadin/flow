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
package com.vaadin.flow.component.polymertemplate;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import com.vaadin.flow.component.EventData;
import com.vaadin.flow.dom.DisabledUpdateMode;

/**
 * Publishes the annotated method so it can be invoked from the client side as
 * template event handlers.
 *
 * @see EventData
 * @author Vaadin Ltd
 * @since 1.0
 *
 * @deprecated Event handlers are not supported by Lit templates. Add a
 *             corresponding DOM listener from server side API instead or fire a
 *             custom event from client side template. Also you may handle an
 *             event directly on the client side and the server side may be
 *             called from this handler via {@code this.$server._some_method}.
 *             Polymer template support is deprecated - we recommend you to use
 *             {@code LitTemplate} instead. Read more details from <a href=
 *             "https://vaadin.com/blog/future-of-html-templates-in-vaadin">the
 *             Vaadin blog.</a>
 */
@Deprecated
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
@Documented
public @interface EventHandler {

    /**
     * Controls event handling for the method from the client side to the server
     * side when the element is disabled.
     *
     * @return the property update mode for disabled element
     */
    DisabledUpdateMode value() default DisabledUpdateMode.ONLY_WHEN_ENABLED;
}
