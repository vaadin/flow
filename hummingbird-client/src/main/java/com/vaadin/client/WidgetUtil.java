/*
 * Copyright 2000-2016 Vaadin Ltd.
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

package com.vaadin.client;

import com.google.gwt.dom.client.AnchorElement;
import com.google.gwt.dom.client.Document;

/**
 * Utility methods which are related to client side code only
 */
public class WidgetUtil {

    /**
     * Redirects the browser to the given url or refreshes the page if url is
     * null
     *
     * @since 7.6
     * @param url
     *            The url to redirect to or null to refresh
     */
    public static native void redirect(String url)
    /*-{
        if (url) {
                $wnd.location = url;
        } else {
                $wnd.location.reload(false);
        }
    }-*/;

    /**
     * Resolve a relative URL to an absolute URL based on the current document's
     * location.
     *
     * @param url
     *            a string with the relative URL to resolve
     * @return the corresponding absolute URL as a string
     */
    public static String getAbsoluteUrl(String url) {
        AnchorElement a = Document.get().createAnchorElement();
        a.setHref(url);
        return a.getHref();
    }

    /**
     * Anything in, anything out. It's JavaScript after all. This method just
     * makes the Java compiler accept the fact.
     *
     * @param value
     *            anything
     * @return the same stuff
     */
    public static native <T> T crazyJsCast(Object value)
    /*-{
        return value;
    }-*/;
}
