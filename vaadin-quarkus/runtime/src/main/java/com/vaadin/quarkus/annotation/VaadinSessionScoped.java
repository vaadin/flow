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
package com.vaadin.quarkus.annotation;

import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import jakarta.enterprise.context.NormalScope;

import com.vaadin.flow.server.VaadinSession;

import static java.lang.annotation.ElementType.ANNOTATION_TYPE;
import static java.lang.annotation.ElementType.CONSTRUCTOR;
import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

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
 * @since 1.0
 */
@NormalScope
@Inherited
@Target({ ANNOTATION_TYPE, TYPE, FIELD, METHOD, CONSTRUCTOR })
@Retention(RUNTIME)
public @interface VaadinSessionScoped {
}
