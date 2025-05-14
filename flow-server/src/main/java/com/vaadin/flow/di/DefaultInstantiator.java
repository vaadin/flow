/*
 * Copyright 2000-2025 Vaadin Ltd.
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

import java.util.List;
import java.util.Locale;
import java.util.ServiceLoader;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.function.DeploymentConfiguration;
import com.vaadin.flow.function.SerializableSupplier;
import com.vaadin.flow.i18n.DefaultI18NProvider;
import com.vaadin.flow.i18n.I18NProvider;
import com.vaadin.flow.i18n.I18NUtil;
import com.vaadin.flow.internal.ReflectTools;
import com.vaadin.flow.server.InitParameters;
import com.vaadin.flow.server.InvalidI18NConfigurationException;
import com.vaadin.flow.server.InvalidMenuAccessControlException;
import com.vaadin.flow.server.VaadinService;
import com.vaadin.flow.server.VaadinServiceInitListener;
import com.vaadin.flow.server.auth.DefaultMenuAccessControl;
import com.vaadin.flow.server.auth.MenuAccessControl;

/**
 * Default instantiator that is used if no other instantiator has been
 * registered. This implementation uses vanilla Java mechanisms such as
 * {@link Class#newInstance()} and {@link ServiceLoader}.
 *
 * @author Vaadin Ltd
 * @since 1.0
 */
public class DefaultInstantiator implements Instantiator {
    private VaadinService service;
    private static final AtomicReference<I18NProvider> i18nProvider = new AtomicReference<>();
    private static final AtomicReference<MenuAccessControl> menuAccessControl = new AtomicReference<>();

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
    public Stream<VaadinServiceInitListener> getServiceInitListeners() {
        return getServiceLoaderListeners(service.getClassLoader());
    }

    @Override
    public <T> T getOrCreate(Class<T> type) {
        Lookup lookup = service.getContext().getAttribute(Lookup.class);
        T result = lookup == null ? null : lookup.lookup(type);
        return result == null ? create(type) : result;
    }

    @Override
    public <T extends Component> T createComponent(Class<T> componentClass) {
        return create(componentClass);
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
    public I18NProvider getI18NProvider() {
        return getAtomicReferenceInstance(i18nProvider,
                this::getI18NProviderInstance);
    }

    @Override
    public MenuAccessControl getMenuAccessControl() {
        return getAtomicReferenceInstance(menuAccessControl,
                this::getMenuAccessControlInstance);
    }

    private <T> T getAtomicReferenceInstance(AtomicReference<T> reference,
            SerializableSupplier<T> instance) {
        if (reference.get() == null) {
            reference.compareAndSet(null, instance.get());
        }
        return reference.get();
    }

    private I18NProvider getI18NProviderInstance() {
        String property = getInitProperty(InitParameters.I18N_PROVIDER);
        if (property == null) {
            // If no i18n provider provided check if the default location has
            // translation files (lang coded or just the default)
            List<Locale> defaultTranslationLocales = I18NUtil
                    .getDefaultTranslationLocales(getClassLoader());
            if (!defaultTranslationLocales.isEmpty()
                    || I18NUtil.containsDefaultTranslation(getClassLoader())) {
                // Some lang files were found in default location initialize
                // default I18N provider.
                return new DefaultI18NProvider(defaultTranslationLocales,
                        getClassLoader());
            }
            return null;
        }
        try {
            // Get i18n provider class if found in application
            // properties
            Class<?> providerClass = getClassLoader().loadClass(property);
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

    private MenuAccessControl getMenuAccessControlInstance() {
        String property = getInitProperty(InitParameters.MENU_ACCESS_CONTROL);
        if (property == null) {
            return new DefaultMenuAccessControl();
        }
        try {
            // Get Menu Access Control class if found in application
            // properties
            Class<?> providerClass = getClassLoader().loadClass(property);
            if (MenuAccessControl.class.isAssignableFrom(providerClass)) {

                return ReflectTools.createInstance(
                        (Class<? extends MenuAccessControl>) providerClass);
            } else {
                throw new InvalidMenuAccessControlException(String.format(
                        "Menu access control implementation class property '%s' is set to '%s' but it's not %s implementation",
                        InitParameters.MENU_ACCESS_CONTROL, property,
                        MenuAccessControl.class.getSimpleName()));
            }
        } catch (ClassNotFoundException e) {
            throw new InvalidMenuAccessControlException(
                    "Failed to load given provider class '" + property
                            + "' as it was not found by the class loader.",
                    e);
        }
    }

    protected ClassLoader getClassLoader() {
        // Use the application thread ClassLoader to invalidate ResourceBundle
        // cache on dev mode reload. See
        // https://github.com/vaadin/hilla/issues/2554
        return Thread.currentThread().getContextClassLoader();
    }

    /**
     * Get property value from the session configurator or try to load it from
     * application.properties property file.
     *
     * @return parameter value or null if not found
     */
    protected String getInitProperty(String propertyName) {
        DeploymentConfiguration deploymentConfiguration = service
                .getDeploymentConfiguration();
        if (deploymentConfiguration == null) {
            return null;
        }
        return deploymentConfiguration.getStringProperty(propertyName, null);
    }

    private <T> T create(Class<T> type) {
        return ReflectTools.createInstance(type);
    }
}
