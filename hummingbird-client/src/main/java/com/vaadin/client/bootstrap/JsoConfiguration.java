package com.vaadin.client.bootstrap;

import com.google.gwt.core.client.JavaScriptObject;

/**
 * Helper class for reading configuration options from the bootstap javascript
 *
 * @since 7.0
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
     * Reads a configuration parameter as a boolean object. Please note that the
     * javascript value of the parameter should also be a boolean, or else an
     * undefined exception may be thrown.
     *
     * @param name
     *            name of the configuration parameter
     * @return boolean value of the configuration paramter, or <code>null</code>
     *         if no value is defined
     */
    public native Boolean getConfigBoolean(String name)
    /*-{
        var value = this.getConfig(name);
        if (value === null || value === undefined) {
            return null;
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
     * @return integer value of the configuration paramter, or <code>null</code>
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
     * Returns a native javascript object containing version information from
     * the server.
     *
     * @return a javascript object with the version information
     */
    public native JavaScriptObject getVersionInfoJSObject()
    /*-{
        return this.getConfig("versionInfo");
    }-*/;

    /**
     * Gets the version of the Vaadin framework used on the server.
     *
     * @return a string with the version
     *
     * @see com.vaadin.server.VaadinServlet#VERSION
     */
    public native String getVaadinVersion()
    /*-{
        return this.getConfig("versionInfo").vaadinVersion;
    }-*/;

    /**
     * Gets the version of the Atmosphere framework.
     *
     * @return a string with the version
     *
     * @see org.atmosphere.util#getRawVersion()
     */
    public native String getAtmosphereVersion()
    /*-{
        return this.getConfig("versionInfo").atmosphereVersion;
    }-*/;

    /**
     * Gets the JS version used in the Atmosphere framework.
     *
     * @return a string with the version
     */
    public native String getAtmosphereJSVersion()
    /*-{
        if ($wnd.jQueryVaadin != undefined){
            return $wnd.jQueryVaadin.atmosphere.version;
        }
        else {
            return null;
        }
    }-*/;

    public native String getUIDL()
    /*-{
       return this.getConfig("uidl");
     }-*/;
}
