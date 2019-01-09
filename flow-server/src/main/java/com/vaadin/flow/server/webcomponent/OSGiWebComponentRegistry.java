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
package com.vaadin.flow.server.webcomponent;

import javax.servlet.ServletContext;
import java.util.Map;
import java.util.Optional;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.server.osgi.OSGiAccess;

/**
 * OSGi WebComponentRegistry implementation.
 */
public class OSGiWebComponentRegistry extends WebComponentRegistry {
    boolean hasWebComponents = false;

    @Override
    public boolean setWebComponents(
            Map<String, Class<? extends Component>> components) {
        configurationLock.lock();
        try {
            boolean result = super.setWebComponents(components);
            hasWebComponents = true;
            return result;
        } finally {
            configurationLock.unlock();
        }
    }

    @Override
    public Optional<Class<? extends Component>> getWebComponent(String tag) {
        initWebComponents();
        return super.getWebComponent(tag);
    }

    @Override
    public Map<String, Class<? extends Component>> getWebComponents() {
        initWebComponents();
        return super.getWebComponents();
    }

    private void initWebComponents() {
        configurationLock.lock();
        try {
            Map<String, Class<? extends Component>> webComponents = super
                    .getWebComponents();
            if (webComponents != null && !webComponents.isEmpty()) {
                return;
            }

            if (hasWebComponents) {
                // WebComponents have already been set e.g. by web container which
                // is able to run ServletContainerInitializer
                return;
            }

            ServletContext osgiServletContext = OSGiAccess.getInstance()
                    .getOsgiServletContext();
            if (osgiServletContext == null || !OSGiAccess.getInstance()
                    .hasInitializers()) {
                return;
            }
            OSGiWebComponentDataCollector dataCollector = (OSGiWebComponentDataCollector) getInstance(
                    osgiServletContext);
            if (dataCollector.webComponents.get() != null) {
                setWebComponents(dataCollector.webComponents.get());
            }
        } finally {
            configurationLock.unlock();
        }
    }
}
