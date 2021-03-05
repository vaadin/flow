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
package com.vaadin.flow.osgi.support;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import org.osgi.framework.Bundle;
import org.osgi.framework.FrameworkUtil;
import org.osgi.service.component.annotations.Component;

import com.vaadin.flow.di.ResourceProvider;

/**
 * OSGi capable implementation of {@link ResourceProvider}.
 * 
 * @author Vaadin Ltd
 * @since
 *
 */
@Component(immediate = true, service = ResourceProvider.class)
public class OSGiResourceProvider implements ResourceProvider {

    @Override
    public URL getResource(Class<?> clazz, String path) {
        return FrameworkUtil.getBundle(Objects.requireNonNull(clazz))
                .getResource(path);
    }

    @Override
    public URL getResource(Object context, String path) {
        return getResource(Objects.requireNonNull(context).getClass(), path);
    }

    @Override
    public List<URL> getResources(Class<?> clazz, String path)
            throws IOException {
        return Collections.list(FrameworkUtil
                .getBundle(Objects.requireNonNull(clazz)).getResources(path));
    }

    @Override
    public URL getClientResource(String path) {
        Bundle[] bundles = FrameworkUtil.getBundle(OSGiResourceProvider.class)
                .getBundleContext().getBundles();
        for (Bundle bundle : bundles) {
            if ("com.vaadin.flow.client".equals(bundle.getSymbolicName())) {
                return bundle.getResource(path);
            }
        }
        return null;
    }

    @Override
    public InputStream getClientResourceAsStream(String path)
            throws IOException {
        // No any caching !: flow-client may be reinstalled at any moment
        return getClientResource(path).openStream();
    }
}
