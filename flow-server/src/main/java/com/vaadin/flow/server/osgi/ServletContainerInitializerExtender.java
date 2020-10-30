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
package com.vaadin.flow.server.osgi;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.osgi.util.tracker.BundleTracker;

import com.vaadin.flow.di.ResourceProvider;

/**
 * Bundle activator which starts bundle tracker.
 * 
 * @author Vaadin Ltd
 * @since 1.2
 */
public class ServletContainerInitializerExtender implements BundleActivator {

    private BundleTracker<Bundle> tracker;

    private ServiceRegistration<ResourceProvider> registration;

    @Override
    public void start(BundleContext context) throws Exception {
        registration = context.registerService(ResourceProvider.class,
                new OSGiResourceProvider(), null);
        tracker = new VaadinBundleTracker(context);
        tracker.open();
    }

    @Override
    public void stop(BundleContext context) throws Exception {
        if (registration != null) {
            registration.unregister();
        }
        tracker.close();
        tracker = null;
    }
}
