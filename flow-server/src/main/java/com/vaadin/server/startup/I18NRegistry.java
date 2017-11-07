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
package com.vaadin.server.startup;

import java.util.concurrent.atomic.AtomicReference;

import com.vaadin.ui.i18n.I18NProvider;

/**
 * Registry for holding the I18NProvider that is used for translations and to
 * determine initial locale.
 */
public class I18NRegistry {
    private final AtomicReference<I18NProvider> i18nProvider = new AtomicReference<>();

    private static final I18NRegistry INSTANCE = new I18NRegistry();

    private I18NRegistry() {
    }

    /**
     * Get instance of I18NRegistry.
     *
     * @return singleton instance of the registry
     */
    public static I18NRegistry getInstance() {
        return INSTANCE;
    }

    /**
     * Returns whether this registry has been initialized with a i18n provider.
     *
     * @return whether this registry has been initialized
     */
    public boolean isInitialized() {
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

    /**
     * Set the I18N provider.
     * 
     * @param provider
     *            I18N provider
     */
    public void setProvider(final I18NProvider provider) {
        i18nProvider.set(provider);
    }
}
