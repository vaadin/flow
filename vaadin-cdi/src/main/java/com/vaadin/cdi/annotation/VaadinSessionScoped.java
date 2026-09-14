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
package com.vaadin.cdi.annotation;

import jakarta.enterprise.context.NormalScope;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import com.vaadin.flow.server.VaadinSession;

/**
 * The lifecycle of a VaadinSessionScoped bean is bound to a
 * {@link VaadinSession}.
 * <p>
 * Injecting with this annotation will create a proxy for the contextual
 * instance rather than provide the contextual instance itself.
 * <p>
 * Contextual instances stored in {@link VaadinSession}, so indirectly stored in
 * HTTP session. {@link jakarta.annotation.PreDestroy} called after
 * {@link com.vaadin.flow.server.SessionDestroyEvent} fired.
 *
 * @since 3.0
 */
@NormalScope
@Inherited
@Target({ ElementType.ANNOTATION_TYPE, ElementType.TYPE, ElementType.FIELD,
        ElementType.METHOD, ElementType.CONSTRUCTOR })
@Retention(RetentionPolicy.RUNTIME)
public @interface VaadinSessionScoped {
}
