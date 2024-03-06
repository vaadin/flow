/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.client.bootstrap;

import com.google.gwt.core.client.JavaScriptObject;

/**
 * Wraps a native javascript object containing fields for an error message
 *
 * @since 1.0
 */
public final class ErrorMessage extends JavaScriptObject {

    protected ErrorMessage() {
        // JSO constructor
    }

    public native String getCaption()
    /*-{
        return this.caption;
    }-*/;

    public native String getMessage()
    /*-{
        return this.message;
    }-*/;

    public native String getUrl()
    /*-{
        return this.url;
    }-*/;

    public native String getQuerySelector()
    /*-{
        return this.querySelector;
    }-*/;
}
