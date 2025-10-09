/*
 * Copyright 2000-2025 Vaadin Ltd.
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
package com.vaadin.flow.internal;

import com.vaadin.flow.server.VaadinContext;

/**
 * Allows to access the web application classloader.
 * <p>
 * The functionality is intended to internal usage only. The implementation of
 * this interface may be set as an attribute in {@link VaadinContext} so that
 * the classloader may be used in other place where {@link VaadinContext} is
 * available.
 * <p>
 * For internal use only. May be renamed or removed in a future release.
 *
 * @author Vaadin Ltd
 *
 */
@FunctionalInterface
public interface ApplicationClassLoaderAccess {

    /**
     * Gets the web application classloader.
     *
     * @return the web application classloader.
     */
    ClassLoader getClassloader();
}
