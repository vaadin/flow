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
package com.vaadin.server;

import java.util.concurrent.atomic.AtomicReference;

import com.vaadin.ui.i18n.I18NProvider;
import com.vaadin.util.ReflectTools;

/**
 * Registry for holding the I18NProvider that is used for translations and to
 * determine initial locale.
 */
public class I18NRegistry {
    private final AtomicReference<I18NProvider> i18nProvider = new AtomicReference<>();
    private final AtomicReference<Boolean> initialized = new AtomicReference<>();

    private static final I18NRegistry INSTANCE = new I18NRegistry();

    private I18NRegistry() {
    }

    /**
     * Get instance of I18NRegistry.
     *
     * @return singleton instance of the registry
     */
    public static I18NRegistry getInstance() {
        if (!INSTANCE.isInitialized()) {
            INSTANCE.loadProvider();
        }
        return INSTANCE;
    }

    /**
     * Returns whether this registry has been initialized.
     *
     * @return whether this registry has been initialized
     */
    public boolean isInitialized() {
        return initialized.get() != null;
    }

    /**
     * Returns whether this registry has a i18nProvider.
     *
     * @return whether this registry has a i18nProvider
     */
    public boolean hasProvider() {
        return i18nProvider.get() != null;
    }

    /**
     * Get the registered I18N provider if registered.
     * 
     * @return registered I18N provider or null if none.
     */
    public I18NProvider getProvider() {
        return i18nProvider.get();
    }

    private void loadProvider() {
        if (VaadinSession.getCurrent() == null) {
            // If we try to initialize the provider before we have a servlet
            // we should postpone the initialization.
            return;
        }

        initialized.set(true);

        String property = VaadinSession.getCurrent().getConfiguration()
                .getStringProperty(Constants.I18N_PROVIDER, null);

        if (property == null) {
            return;
        }

        try {
            ClassLoader classLoader = getClass().getClassLoader();
            // Get i18n provider class if found in application properties
            Class<?> providerClass = classLoader.loadClass(property);
            if (I18NProvider.class.isAssignableFrom(providerClass)) {
                setProvider(ReflectTools.createInstance(
                        (Class<? extends I18NProvider>) providerClass));
            }
        } catch (ClassNotFoundException e) {
            throw new InvalidI18NConfigurationException(
                    "Failed to load given provider class '" + property
                            + "' as it was not found by the class loader.",
                    e);
        }
    }

    /**
     * Set the I18N provider.
     * 
     * @param provider
     *            I18N provider
     */
    private void setProvider(final I18NProvider provider) {
        i18nProvider.set(provider);
    }
}
