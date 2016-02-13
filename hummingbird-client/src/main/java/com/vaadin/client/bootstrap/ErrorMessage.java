package com.vaadin.client.bootstrap;

import com.google.gwt.core.client.JavaScriptObject;

/**
 * Wraps a native javascript object containing fields for an error message
 *
 * @since 7.0
 */
public final class ErrorMessage extends JavaScriptObject {

    protected ErrorMessage() {
        // JSO constructor
    }

    public final native String getCaption()
    /*-{
        return this.caption;
    }-*/;

    public final native String getMessage()
    /*-{
        return this.message;
    }-*/;

    public final native String getUrl()
    /*-{
        return this.url;
    }-*/;
}
