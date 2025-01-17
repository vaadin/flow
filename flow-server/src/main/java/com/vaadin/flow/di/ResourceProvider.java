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
package com.vaadin.flow.di;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.List;

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
     * Gets the resource identified by {@code path} located in the application
     * bundle (jar) which may be found using this resource provider instance.
     * <p>
     * If the provider doesn't contain any information about application bundle
     * or there is no resource with the given path then this method returns
     * {@code null}.
     *
     * @param path
     *            the resource path
     * @return an URL of the resource, may be {@code null}
     */
    URL getApplicationResource(String path);

    /**
     * Gets all the resources identified by {@code path} located in in the
     * application bundle (jar) which may be found using this resource provider.
     * <p>
     * If the provider doesn't contain any information about application bundle
     * or there is no resource with the given path then this method returns an
     * empty list.
     *
     * @param path
     *            the resource path
     * @return a list of URLs of the resources or an empty list if resources are
     *         not found
     * @throws IOException
     *             if there is an I/O error
     */
    List<URL> getApplicationResources(String path) throws IOException;

    /**
     * Gets "flow-client" bundle resource identified by the {@code path}.
     *
     * @param path
     *            the resource path
     * @return an URL of the resource, may be {@code null}
     */
    URL getClientResource(String path);

    /**
     * Gets "flow-client" bundle resource content identified by the
     * {@code path}.
     *
     * @param path
     *            the resource path
     * @return an URL of the resource, may be {@code null}
     * @throws IOException
     *             If there is an I/O error.
     */
    InputStream getClientResourceAsStream(String path) throws IOException;
}
