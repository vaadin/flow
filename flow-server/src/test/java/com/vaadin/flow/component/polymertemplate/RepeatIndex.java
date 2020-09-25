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

package com.vaadin.flow.component.polymertemplate;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import com.vaadin.flow.component.EventData;
import com.vaadin.flow.component.littemplate.LitTemplate;

/**
 * Allows to receive index of an element in dom-repeat Polymer template section.
 *
 * Can be applied on parameters of {@code int} and {@link Integer} types.
 *
 * This is a shorthand for {@code @EventData("event.model.index")}, for more
 * information, refer to {@link EventData}.
 *
 * @author Vaadin Ltd
 * @since 1.0
 * @deprecated dom-repeat is not support by Lit templates but you may still use
 *             {@code @EventData("some_data")} directly to receive data from the
 *             client side. Polymer template support is deprecated - we
 *             recommend you to use {@link LitTemplate} instead. Read more
 *             details from <a href=
 *             "https://vaadin.com/blog/future-of-html-templates-in-vaadin">the
 *             Vaadin blog.</a>
 */
@Deprecated
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.PARAMETER })
@Documented
public @interface RepeatIndex {
}
