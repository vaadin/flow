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
package com.vaadin.flow.di;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

/**
 * Static "classpath" resources provider.
 * <p>
 * This is SPI to access resources available at runtime. Depending on the web
 * container this can be an application classpath only or bundles which are
 * identified by the provided context.
 * 
 * @author Vaadin Ltd
 * @since
 *
 */
public interface ResourceProvider {

    /**
     * @param clazz
     * @param path
     * @return
     */
    URL getResource(Class<?> clazz, String path);

    /**
     * @param context
     * @param path
     * @return
     */
    URL getResource(Object context, String path);

    /**
     * @param path
     * @return
     */
    URL getClientResource(String path);

    InputStream getClientResourceAsStream(String path) throws IOException;
}
