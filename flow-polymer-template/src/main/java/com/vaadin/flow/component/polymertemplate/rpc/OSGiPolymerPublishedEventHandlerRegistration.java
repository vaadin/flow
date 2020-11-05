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
package com.vaadin.flow.component.polymertemplate.rpc;

import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;

import com.vaadin.flow.component.template.internal.DeprecatedPolymerPublishedEventHandler;
import com.vaadin.flow.di.Lookup;

/**
 * Registers {@link PolymerPublishedEventRpcHandler} as a service to make it
 * available in {@link Lookup} in an OSGi container.
 * 
 * @author Vaadin Ltd
 * @since
 *
 */
@Component(immediate = true)
public class OSGiPolymerPublishedEventHandlerRegistration {

    private ServiceRegistration<DeprecatedPolymerPublishedEventHandler> registration;

    @Activate
    void activate() {
        registration = FrameworkUtil
                .getBundle(OSGiPolymerPublishedEventHandlerRegistration.class)
                .getBundleContext()
                .registerService(DeprecatedPolymerPublishedEventHandler.class,
                        new PolymerPublishedEventRpcHandler(), null);
    }

    @Deactivate
    void deactivate() {
        if (registration != null) {
            registration.unregister();
            registration = null;
        }
    }
}
