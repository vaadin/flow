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
package com.vaadin.flow.server.frontend.scanner;

import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;

import com.vaadin.flow.theme.AbstractTheme;

/**
 * A wrapper for the Theme instance that use reflection for executing its
 * methods. This is needed because updaters can be executed from maven plugins
 * that use different classloaders for the running process and for the project
 * configuration.
 *
 * @since
 */
class ThemeWrapper implements AbstractTheme, Serializable {
    private final Serializable instance;

    public ThemeWrapper(Class<? extends AbstractTheme> theme)
            throws InstantiationException, IllegalAccessException {
        instance = theme.newInstance();
    }

    @Override
    public String getBaseUrl() {
        return invoke(instance, "getBaseUrl");
    }

    @Override
    public String getThemeUrl() {
        return invoke(instance, "getThemeUrl");
    }

    @Override
    public Map<String, String> getHtmlAttributes(String variant) {
        return invoke(instance, "getHtmlAttributes", variant);
    }

    @Override
    public List<String> getHeaderInlineContents() {
        return invoke(instance, "getHeaderInlineContents");
    }

    @Override
    public String translateUrl(String url) {
        return invoke(instance, "translateUrl", url);
    }

    @SuppressWarnings("unchecked")
    private <T> T invoke(Object instance, String methodName,
            Object... arguments) {
        try {
            for (Method m : instance.getClass().getMethods()) {
                if (m.getName().equals(methodName)) {
                    return (T) m.invoke(instance, arguments);
                }
            }
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new IllegalArgumentException(e);
        }
        return null;
    }
}
