/**
 * Copyright (C) 2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See {@literal <https://vaadin.com/commercial-license-and-service-terms>}  for the full
 * license.
 */
package com.vaadin.flow.di;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.List;

import com.vaadin.flow.server.VaadinContext;

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
     * Gets all the web application resources identified by the {@code path}
     * using the provided {@code context}.
     *
     * @param context
     *            a context object
     * @param path
     *            the resource path
     * @return a list of URLs of the resources or an empty list if resources are
     *         not found
     *
     * @throws IOException
     *             if there is an I/O error
     */
    List<URL> getApplicationResources(VaadinContext context, String path)
            throws IOException;

    /**
     * Gets the web application resource identified by the {@code path} using
     * the provided {@code context}.
     *
     * @param context
     *            a context object
     * @param path
     *            the resource path
     * @return an URL of the resource, may be {@code null}
     */
    URL getApplicationResource(VaadinContext context, String path);

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
