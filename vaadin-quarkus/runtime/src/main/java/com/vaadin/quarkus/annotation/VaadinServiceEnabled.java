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

import java.io.Serial;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import jakarta.enterprise.util.AnnotationLiteral;
import jakarta.inject.Qualifier;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Qualifier to mark Vaadin service implementations.
 *
 * Qualified CDI beans implementing {@link com.vaadin.flow.i18n.I18NProvider},
 * and {@link com.vaadin.flow.di.InstantiatorFactory} interfaces are loaded.
 */
@Qualifier
@Retention(RUNTIME)
@Target({ TYPE, METHOD, FIELD, PARAMETER })
public @interface VaadinServiceEnabled {

    /**
     * Supports inline instantiation of the {@link VaadinServiceEnabled}
     * annotation.
     *
     * @since 2.2
     */
    final class Literal extends AnnotationLiteral<VaadinServiceEnabled>
            implements VaadinServiceEnabled {
        /**
         * Singleton instance of the {@code Literal} class, which allows inline
         * instantiation of the {@link VaadinServiceEnabled} annotation. This
         * instance provides a reusable, predefined implementation of the
         * annotation for use in various contexts.
         */
        public static final Literal INSTANCE = new Literal();

        @Serial
        private static final long serialVersionUID = 1L;
    }

}
