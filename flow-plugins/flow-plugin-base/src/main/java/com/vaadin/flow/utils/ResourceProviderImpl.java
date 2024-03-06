/*
 * Copyright (C) 2000-2024 Vaadin Ltd
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
import java.util.Collections;
import java.util.List;

import com.vaadin.flow.di.ResourceProvider;

/**
 * {@link ResourceProvider} for use with plugin execution.
 *
 * @author Vaadin Ltd
 * @since
 */
class ResourceProviderImpl implements ResourceProvider {

    @Override
    public URL getApplicationResource(String path) {
        return ResourceProviderImpl.class.getClassLoader().getResource(path);
    }

    @Override
    public List<URL> getApplicationResources(String path) throws IOException {
        return Collections.list(
                ResourceProviderImpl.class.getClassLoader().getResources(path));
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
