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
package com.vaadin.client.bootstrap;

import com.google.gwt.core.client.JavaScriptObject;
import com.vaadin.client.ValueMap;

/**
 * Helper class for reading configuration options from the bootstrap javascript.
 *
 * @since 1.0
 */
public final class JsoConfiguration extends JavaScriptObject {
    protected JsoConfiguration() {
        // JSO Constructor
    }

    /**
     * Reads a configuration parameter as a string. Please note that the
     * javascript value of the parameter should also be a string, or else an
     * undefined exception may be thrown.
     *
     * @param name
     *            name of the configuration parameter
     * @return value of the configuration parameter, or <code>null</code> if not
     *         defined
     */
    public native String getConfigString(String name)
    /*-{
        var value = this.getConfig(name);
        if (value === null || value === undefined) {
            return null;
        } else {
            return value +"";
        }
    }-*/;

    /**
     * Reads a configuration parameter as a {@link ValueMap}. Please note that
     * the javascript value of the parameter should also be a javascript object,
     * or else an undefined exception may be thrown.
     *
     * @param name
     *            name of the configuration parameter
     * @return value of the configuration parameter, or <code>null</code>if not
     *         defined
     */
    public native ValueMap getConfigValueMap(String name)
    /*-{
        return this.getConfig(name);
    }-*/;

    /**
     * Reads a configuration parameter as a String array.
     *
     * @param name
     *            name of the configuration parameter
     * @return value of the configuration parameter, or <code>null</code>if not
     *         defined
     */
    public native String[] getConfigStringArray(String name)
    /*-{
        return this.getConfig(name);
    }-*/;

    /**
     * Reads a configuration parameter as a boolean.
     * <p>
     * Please note that the javascript value of the parameter should also be a
     * boolean, or else an undefined exception may be thrown.
     *
     * @param name
     *            name of the configuration parameter
     * @return the boolean value of the configuration parameter, or
     *         <code>false</code> if no value is defined
     */
    public native boolean getConfigBoolean(String name)
    /*-{
        var value = this.getConfig(name);
        if (value === null || value === undefined) {
            return false;
        } else {
             // $entry not needed as function is not exported
            return @java.lang.Boolean::valueOf(Z)(value);
        }
    }-*/;

    /**
     * Reads a configuration parameter as an integer object. Please note that
     * the javascript value of the parameter should also be an integer, or else
     * an undefined exception may be thrown.
     *
     * @param name
     *            name of the configuration parameter
     * @return integer value of the configuration parameter, or <code>null</code>
     *         if no value is defined
     */
    public native Integer getConfigInteger(String name)
    /*-{
        var value = this.getConfig(name);
        if (value === null || value === undefined) {
            return null;
        } else {
             // $entry not needed as function is not exported
            return @java.lang.Integer::valueOf(I)(value);
        }
    }-*/;

    /**
     * Reads a configuration parameter as an {@link ErrorMessage} object. Please
     * note that the javascript value of the parameter should also be an object
     * with appropriate fields, or else an undefined exception may be thrown
     * when calling this method or when calling methods on the returned object.
     *
     * @param name
     *            name of the configuration parameter
     * @return error message with the given name, or <code>null</code> if no
     *         value is defined
     */
    public native ErrorMessage getConfigError(String name)
    /*-{
        return this.getConfig(name);
    }-*/;

    /**
     * Gets the version of the Vaadin framework used on the server.
     *
     * @return a string with the version
     */
    public native String getVaadinVersion()
    /*-{
        var info = this.getConfig("versionInfo");
        return info ? info.vaadinVersion : null;
    }-*/;

    /**
     * Gets the version of the Atmosphere framework.
     *
     * @return a string with the version
     */
    public native String getAtmosphereVersion()
    /*-{
        var info = this.getConfig("versionInfo");
        return info ? info.atmosphereVersion : null;
    }-*/;

    /**
     * Gets the JS version used in the Atmosphere framework.
     *
     * @return a string with the version
     */
    public native String getAtmosphereJSVersion()
    /*-{
        if (@com.vaadin.client.communication.AtmospherePushConnection::isAtmosphereLoaded()()) {
            return $wnd.vaadinPush.atmosphere.version;
        } else {
            return null;
        }
    }-*/;

    /**
     * Gets the initial UIDL from the bootstrap page.
     *
     * @return the initial UIDL
     */
    public ValueMap getUIDL() {
        return getConfigValueMap("uidl");
    }
}
