/*
 * Copyright 2000-2018 Vaadin Ltd.
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
package com.vaadin.flow.client;

import java.io.InputStream;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceReference;
import org.slf4j.LoggerFactory;

/**
 * Utilities to load client access resources from the client side module.
 *
 * @author Vaadin Ltd
 * @since 1.2
 */
public final class ClientResourcesUtils {

    private static final ClientResources SERVICE = loadService();

    private ClientResourcesUtils() {
        // Avoid instantiation of util class.
    }

    /**
     * Get content of the resource in the client-side module.
     *
     * @param path
     *            the resource path
     * @return the content of the resource as InutStream or null if there is no
     *         resource with the {@code path}
     */
    public static InputStream getResource(String path) {
        return SERVICE.getResource(path);
    }

    private static ClientResources loadService() {
        try {
            Class.forName("org.osgi.framework.FrameworkUtil");
            Bundle bundle = FrameworkUtil.getBundle(ClientResources.class);
            if (bundle == null) {
                return getDefaultService(null);
            }
            BundleContext context = bundle.getBundleContext();

            ServiceReference<ClientResources> reference = context
                    .getServiceReference(ClientResources.class);
            if (reference == null) {
                return getDefaultService(null);
            }
            LoggerFactory.getLogger(ClientResourcesUtils.class).trace(
                    "OSGi environment is detected. Load client resources using OSGi service");
            return context.getService(reference);
        } catch (ClassNotFoundException exception) {
            return getDefaultService(exception);
        }
    }

    private static ClientResources getDefaultService(
            ClassNotFoundException exception) {
        LoggerFactory.getLogger(ClientResourcesUtils.class)
                .trace("Using standard Java way to access "
                        + "to the client resources", exception);
        return new DefaultClientResources();
    }
}
