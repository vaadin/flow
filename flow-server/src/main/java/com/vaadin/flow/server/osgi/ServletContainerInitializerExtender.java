/**
 * Copyright (C) 2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.server.osgi;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.util.tracker.BundleTracker;

/**
 * Bundle activator which starts bundle tracker.
 * <p>
 * For internal use only. May be renamed or removed in a future release.
 *
 *
 * @author Vaadin Ltd
 * @since 1.2
 */
public class ServletContainerInitializerExtender implements BundleActivator {

    private BundleTracker<Bundle> tracker;

    @Override
    public void start(BundleContext context) throws Exception {
        tracker = new VaadinBundleTracker(context);
        tracker.open();
    }

    @Override
    public void stop(BundleContext context) throws Exception {
        tracker.close();
        tracker = null;
    }
}
