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
}
