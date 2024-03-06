/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
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
