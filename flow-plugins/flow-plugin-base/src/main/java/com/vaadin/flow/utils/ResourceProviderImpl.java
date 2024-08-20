/*
 * Copyright 2000-2024 Vaadin Ltd.
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
