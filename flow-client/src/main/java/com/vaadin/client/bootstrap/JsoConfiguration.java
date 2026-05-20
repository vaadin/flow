/*
 * Copyright 2000-2026 Vaadin Ltd.
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
package com.vaadin.client.bootstrap;

import jsinterop.annotations.JsOverlay;
import jsinterop.annotations.JsPackage;
import jsinterop.annotations.JsType;

import com.vaadin.client.ValueMap;
import com.vaadin.client.WidgetUtil;
import com.vaadin.client.communication.AtmospherePushConnection;

/**
 * Helper class for reading configuration options from the bootstrap javascript.
 *
 * @since 1.0
 */
@JsType(isNative = true, namespace = JsPackage.GLOBAL, name = "Object")
public final class JsoConfiguration {

    /**
     * Calls the {@code getConfig(name)} method that the bootstrap JavaScript
     * defines on the configuration object.
     */
    native Object getConfig(String name);

    /**
     * Reads a configuration parameter as a string. Returns {@code null} if the
     * parameter is undefined.
     */
    @JsOverlay
    public final String getConfigString(String name) {
        Object value = getConfig(name);
        if (value == null) {
            return null;
        }
        return value + "";
    }

    /**
     * Reads a configuration parameter as a {@link ValueMap}.
     */
    @JsOverlay
    public final ValueMap getConfigValueMap(String name) {
        return (ValueMap) getConfig(name);
    }

    /**
     * Reads a configuration parameter as a String array.
     */
    @JsOverlay
    public final String[] getConfigStringArray(String name) {
        return WidgetUtil.crazyJsCast(getConfig(name));
    }

    /**
     * Reads a configuration parameter as a boolean. Returns {@code false} when
     * the parameter is undefined.
     */
    @JsOverlay
    public final boolean getConfigBoolean(String name) {
        Object value = getConfig(name);
        if (value == null) {
            return false;
        }
        return ((Boolean) value).booleanValue();
    }

    /**
     * Reads a configuration parameter as an integer object. Returns
     * {@code null} when the parameter is undefined.
     */
    @JsOverlay
    public final Integer getConfigInteger(String name) {
        Object value = getConfig(name);
        if (value == null) {
            return null;
        }
        return Integer.valueOf(((Number) value).intValue());
    }

    /**
     * Reads a configuration parameter as an {@link ErrorMessage}.
     */
    @JsOverlay
    public final ErrorMessage getConfigError(String name) {
        return (ErrorMessage) getConfig(name);
    }

    /**
     * Gets the version of the Vaadin framework used on the server.
     */
    @JsOverlay
    public final String getVaadinVersion() {
        Object info = getConfig("versionInfo");
        if (info == null) {
            return null;
        }
        return (String) WidgetUtil.getJsProperty(info, "vaadinVersion");
    }

    /**
     * Gets the version of the Atmosphere framework.
     */
    @JsOverlay
    public final String getAtmosphereVersion() {
        Object info = getConfig("versionInfo");
        if (info == null) {
            return null;
        }
        return (String) WidgetUtil.getJsProperty(info, "atmosphereVersion");
    }

    /**
     * Gets the JS version used in the Atmosphere framework.
     */
    @JsOverlay
    public final String getAtmosphereJSVersion() {
        if (AtmospherePushConnection.isAtmosphereLoaded()) {
            return AtmospherePushConnection.getAtmosphereJSVersion();
        }
        return null;
    }

    /**
     * Gets the initial UIDL from the bootstrap page.
     */
    @JsOverlay
    public final ValueMap getUIDL() {
        return getConfigValueMap("uidl");
    }
}
