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
package com.vaadin.guice.annotation;

import com.google.inject.Module;
import com.vaadin.server.RequestHandler;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * This annotation is to provide one or more packages that should be scanned for
 * {@link com.vaadin.server.BootstrapListener}s, {@link RequestHandler}s
 * and {@link Module}s. Every {@link com.vaadin.guice.server.GuiceVaadinServlet}
 * must either have this annotation attached ot have an initParam named
 * 'packagesToScan' in its web.xml
 */
@Target({java.lang.annotation.ElementType.TYPE})
@Retention(java.lang.annotation.RetentionPolicy.RUNTIME)
@Documented
public @interface PackagesToScan {
    /**
     * the packages to scan
     */
    String[] value();
}
