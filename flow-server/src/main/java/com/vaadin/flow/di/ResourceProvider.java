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
import java.util.List;

import com.vaadin.flow.server.VaadinService;
import com.vaadin.flow.server.VaadinServlet;

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
     * bundle (jar) which may be found using the {@code contextClass}.
     * <p>
     * If the {@code contextClass} doesn't contain any information about
     * application bundle or there is no resource with the given path then this
     * method returns {@code null}.
     * 
     * @param contextClass
     *            a class to find an application bundle
     * @param path
     *            the resource path
     * @return an URL of the resource, may be {@code null}
     */
    URL getApplicationResource(Class<?> contextClass, String path);

    /**
     * Gets all the resources identified by {@code path} located in in the
     * application bundle (jar) which may be found using the
     * {@code contextClass}.
     * <p>
     * If the {@code contextClass} doesn't contain any information about
     * application bundle or there is no resource with the given path then this
     * method returns an empty list.
     * 
     * @param contextClass
     *            a class to find an application bundle
     * @param path
     *            the resource path
     * @return a list of URLs of the resources or an empty list if resources are
     *         not found
     */
    List<URL> getApplicationResources(Class<?> contextClass, String path)
            throws IOException;

    /**
     * Gets all the web application resources identified by the {@code path}
     * using the provided {@code context}.
     * <p>
     * The context doesn't have to be in the same bundle as the resource. The
     * resource location is the web application bundle (WAR or WAB) which might
     * not contain the {@code context} declaration at all. The {@code context}
     * instance is only used to find the correct bundle where the resource is
     * located.
     * <p>
     * A typical scenario could be : the {@code context} object is a
     * {@link VaadinService} or {@link VaadinServlet} instance and the path is
     * some resource in the web application (which is very specific for the
     * application), e.g. {@code "stats.json"}. Both {@link VaadinService} and
     * {@link VaadinServlet} are located in "flow-server" bundle which doesn't
     * know anything about web app specific resources. If a servlet is
     * registered manually in the WAB then its bundle may be used to find the
     * resource. Otherwise some context information about WAB should be
     * retrieved from the {@code context}.
     * 
     * @param context
     *            a context object
     * @param path
     *            the resource path
     * @return a list of URLs of the resources or an empty list if resources are
     *         not found
     */
    List<URL> getApplicationResources(Object context, String path)
            throws IOException;

    /**
     * Gets the web application resource identified by the {@code path} using
     * the provided {@code context}.
     * <p>
     * The context doesn't have to be in the same bundle as the resource. The
     * resource location is the web application bundle (WAR or WAB) which might
     * not contain the {@code context} declaration at all. The {@code context}
     * instance is only used to find the correct bundle where the resource is
     * located.
     * <p>
     * A typical scenario could be : the {@code context} object is a
     * {@link VaadinService} or {@link VaadinServlet} instance and the path is
     * some resource in the web application (which is very specific for the
     * application), e.g. {@code "stats.json"}. Both {@link VaadinService} and
     * {@link VaadinServlet} are located in "flow-server" bundle which doesn't
     * know anything about web app specific resources. If a servlet is
     * registered manually in the WAB then its bundle may be used to find the
     * resource. Otherwise some context information about WAB should be
     * retrieved from the {@code context}.
     * 
     * @param context
     *            a context object
     * @param path
     *            the resource path
     * @return an URL of the resource, may be {@code null}
     */
    URL getApplicationResource(Object context, String path);

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
