/*
 * Copyright (C) 2000-2026 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.utils;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Collections;
import java.util.List;

import com.vaadin.flow.di.ResourceProvider;
import com.vaadin.flow.server.frontend.scanner.ClassFinder;

/**
 * {@link ResourceProvider} for use with plugin execution.
 *
 * @author Vaadin Ltd
 * @since
 */
class ResourceProviderImpl implements ResourceProvider {

    private ClassLoader classLoader = null;

    ResourceProviderImpl() {
        this.classLoader = ResourceProviderImpl.class.getClassLoader();
    }

    ResourceProviderImpl(ClassFinder classFinder) {
        this.classLoader = classFinder.getClassLoader();
    }

    @Override
    public URL getApplicationResource(String path) {
        return classLoader.getResource(path);
    }

    @Override
    public List<URL> getApplicationResources(String path) throws IOException {
        return Collections.list(classLoader.getResources(path));
    }

    @Override
    public URL getClientResource(String path) {
        throw new UnsupportedOperationException(
                "Client resources are not available in plugin");
    }

    @Override
    public InputStream getClientResourceAsStream(String path) {
        throw new UnsupportedOperationException(
                "Client resources are not available in plugin");
    }
}
