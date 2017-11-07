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

import javax.servlet.ServletContainerInitializer;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.annotation.HandlesTypes;
import java.util.Locale;
import java.util.Set;

import com.vaadin.ui.i18n.I18NProvider;
import com.vaadin.util.ReflectTools;

/**
 * Servlet initalizer for registering a I18N provider for this application.
 */
@HandlesTypes(I18NProvider.class)
public class I18NRegistryInitializer implements ServletContainerInitializer {

    @Override
    public void onStartup(Set<Class<?>> i18nProviders, ServletContext ctx)
            throws ServletException {
        if (i18nProviders == null) {
            I18NRegistry.getInstance().setProvider(new DefaultProvider());
            return;
        }

        i18nProviders.remove(DefaultProvider.class);

        if (i18nProviders.size() > 1) {
            throw new IllegalStateException(
                    "Only one I18NProvider should be defined.");
        }

        if (!i18nProviders.isEmpty()) {
            Class<? extends I18NProvider> provider = (Class<? extends I18NProvider>) i18nProviders
                    .iterator().next();
            I18NRegistry.getInstance()
                    .setProvider(ReflectTools.createInstance(provider));
        } else {
            I18NRegistry.getInstance().setProvider(new DefaultProvider());
        }

    }

    /**
     * Default no-op provider if no i18n provider exists.
     */
    private static class DefaultProvider implements I18NProvider {
        @Override
        public String getTranslation(String key, Object... params) {
            throw new UnsupportedOperationException(
                    "Implement an I18NProvider to get translation support.");
        }

        @Override
        public String getTranslation(String key, Locale locale,
                Object... params) {
            throw new UnsupportedOperationException(
                    "Implement an I18NProvider to get translation support.");
        }
    }
}
