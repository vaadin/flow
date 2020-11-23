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
import org.osgi.framework.BundleContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.util.tracker.BundleTracker;

/**
 * Service to start bundle tracker.
 * 
 * @author Vaadin Ltd
 * @since 1.2
 */
@Component(immediate = true)
public class ServletContainerInitializerExtender {

    private BundleTracker<Bundle> tracker;

    /**
     * Activates the component.
     * 
     * @param context
     *            the provided bundle context
     */
    @Activate
    public void activate(BundleContext context) {
        tracker = new VaadinBundleTracker(context);
        tracker.open();
    }

    /**
     * Deactivate the component.
     */
    @Deactivate
    public void deactivate() {
        if (tracker != null) {
            tracker.close();
            tracker = null;
        }
    }
}
