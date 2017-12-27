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
package com.vaadin.flow.spring.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import com.vaadin.flow.spring.VaadinScopesConfig;
import com.vaadin.flow.spring.VaadinServletConfiguration;

/**
 * Brings in the machinery to setup Spring + Vaadin applications. This
 * annotation should be added on a {@link Configuration} class of the
 * application to automatically import Vaadin configuration (such as
 * {@link VaadinScopesConfig}).
 * <p>
 * No need to use it with Spring Boot application since it enables Vaadin
 * automatically via autoconfiguration.
 *
 * @author Vaadin Ltd
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Import({ VaadinScopesConfig.class, VaadinServletConfiguration.class })
public @interface EnableVaadin {
}
