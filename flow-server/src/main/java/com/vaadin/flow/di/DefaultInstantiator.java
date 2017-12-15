/*
 * Copyright 2000-2017 Vaadin Ltd.
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
package com.vaadin.flow.di;

import java.util.ServiceLoader;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import com.vaadin.function.DeploymentConfiguration;
import com.vaadin.flow.router.NavigationEvent;
import com.vaadin.server.Constants;
import com.vaadin.server.InvalidI18NConfigurationException;
import com.vaadin.server.VaadinService;
import com.vaadin.server.VaadinServiceInitListener;
import com.vaadin.ui.Component;
import com.vaadin.ui.common.HasElement;
import com.vaadin.ui.i18n.I18NProvider;
import com.vaadin.util.ReflectTools;

/**
 * Default instantiator that is used if no other instantiator has been
 * registered. This implementation uses vanilla Java mechanisms such as
 * {@link Class#newInstance()} and {@link ServiceLoader}.
 *
 * @author Vaadin Ltd
 */
public class DefaultInstantiator implements Instantiator {
    private VaadinService service;
    private static final AtomicReference<I18NProvider> i18nProvider = new AtomicReference<>();

    /**
     * Creates a new instantiator for the given service.
     *
     * @param service
     *            the service to use
     */
    public DefaultInstantiator(VaadinService service) {
        this.service = service;
    }

    @Override
    public boolean init(VaadinService service) {
        return service == this.service;
    }

    @Override
    public Stream<VaadinServiceInitListener> getServiceInitListeners() {
        return getServiceLoaderListeners(service.getClassLoader());
    }

    /**
     * Helper for finding service init listeners using {@link ServiceLoader}.
     *
     * @param classloader
     *            the classloader to use for finding the listeners
     * @return a stream of service init listeners
     */
    public static Stream<VaadinServiceInitListener> getServiceLoaderListeners(
            ClassLoader classloader) {
        ServiceLoader<VaadinServiceInitListener> loader = ServiceLoader
                .load(VaadinServiceInitListener.class, classloader);
        return StreamSupport.stream(loader.spliterator(), false);
    }

    @Override
    public <T extends HasElement> T createRouteTarget(Class<T> routeTargetType,
            NavigationEvent event) {
        return ReflectTools.createInstance(routeTargetType);
    }

    @Override
    public <T extends Component> T createComponent(Class<T> componentClass) {
        return ReflectTools.createInstance(componentClass);
    }

    @Override
    public I18NProvider getI18NProvider() {
        if (i18nProvider.get() == null) {
            i18nProvider.compareAndSet(null, getI18NProviderInstance());
        }
        return i18nProvider.get();
    }

    private I18NProvider getI18NProviderInstance() {
        String property = getI18NProviderProperty();
        if (property == null) {
            return null;
        }
        try {
            // Get i18n provider class if found in application
            // properties
            Class<?> providerClass = DefaultInstantiator.class.getClassLoader()
                    .loadClass(property);
            if (I18NProvider.class.isAssignableFrom(providerClass)) {

                return ReflectTools.createInstance(
                        (Class<? extends I18NProvider>) providerClass);
            }
        } catch (ClassNotFoundException e) {
            throw new InvalidI18NConfigurationException(
                    "Failed to load given provider class '" + property
                            + "' as it was not found by the class loader.",
                    e);
        }
        return null;
    }

    /**
     * Get the I18NProvider property from the session configurator or try to
     * load it from application.properties property file.
     * 
     * @return I18NProvider parameter or null if not found
     */
    private String getI18NProviderProperty() {
        DeploymentConfiguration deploymentConfiguration = service
                .getDeploymentConfiguration();
        if (deploymentConfiguration == null) {
            return null;
        }
        return deploymentConfiguration
                .getStringProperty(Constants.I18N_PROVIDER, null);
    }
}
