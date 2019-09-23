/*
 * Copyright 2010 Google Inc.
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
package com.vaadin.client.gwt.elemental.js.util;

import com.google.gwt.core.client.JavaScriptException;
import com.google.gwt.xhr.client.ReadyStateChangeHandler;
import com.google.gwt.xhr.client.XMLHttpRequest;
import com.vaadin.client.Console;

import elemental.client.Browser;
import elemental.html.Window;

/**
 * A Simpler way to use {@link XMLHttpRequest}.
 *
 * Copied from the GWT project to make get/post return the XmlHttpRequest
 *
 * @since 1.0
 */
public class Xhr {
    /**
     * Notifies when an XHR successfully completes, or errors out.
     */
    public interface Callback {
        /**
         * Called when the request fails.
         *
         * @param xhr
         *            the request object
         * @param exception
         *            the exception which cause the request to fail, or null if
         *            not available
         */
        void onFail(XMLHttpRequest xhr, Exception exception);

        /**
         * Called when the request succeeds.
         *
         * @param xhr
         *            the request object
         */
        void onSuccess(XMLHttpRequest xhr);
    }

    private static class Handler implements ReadyStateChangeHandler {
        private final Callback callback;

        private Handler(Callback callback) {
            this.callback = callback;
        }

        @Override
        public void onReadyStateChange(XMLHttpRequest xhr) {
            if (xhr.getReadyState() == XMLHttpRequest.DONE) {
                if (xhr.getStatus() == 200) {
                    callback.onSuccess(xhr);
                    xhr.clearOnReadyStateChange();
                    return;
                }
                callback.onFail(xhr, null);
                xhr.clearOnReadyStateChange();
            }
        }
    }

    /**
     * Send a GET request to the <code>url</code> and dispatch updates to the
     * <code>callback</code>.
     *
     * @param url
     *            the URL
     * @param callback
     *            the callback to be notified
     * @return a reference to the sent XmlHttpRequest
     */
    public static XMLHttpRequest get(String url, Callback callback) {
        return request(create(), "GET", url, callback);
    }

    /**
     * Send a GET request to the <code>url</code> and dispatch updates to the
     * <code>callback</code>.
     *
     * @param window
     *            the window object used to access the XMLHttpRequest
     *            constructor
     * @param url
     *            the URL
     * @param callback
     *            the callback to be notified
     * @return a reference to the sent XmlHttpRequest
     */
    public static XMLHttpRequest get(Window window, String url,
            Callback callback) {
        return request(create(window), "GET", url, callback);
    }

    /**
     * Send a HEAD request to the <code>url</code> and dispatch updates to the
     * <code>callback</code>.
     *
     * @param url
     *            the URL
     * @param callback
     *            the callback to be notified
     * @return a reference to the sent XmlHttpRequest
     */
    public static XMLHttpRequest head(String url, Callback callback) {
        return request(create(), "HEAD", url, callback);
    }

    /**
     * Send a HEAD request to the <code>url</code> and dispatch updates to the
     * <code>callback</code>.
     *
     * @param window
     *            the window object used to access the XMLHttpRequest
     *            constructor
     * @param url
     *            the URL
     * @param callback
     *            the callback to be notified
     * @return a reference to the sent XmlHttpRequest
     */
    public static XMLHttpRequest head(Window window, String url,
            Callback callback) {
        return request(create(window), "HEAD", url, callback);
    }

    /**
     * Send a POST request to the <code>url</code> and dispatch updates to the
     * <code>callback</code>.
     *
     * @param url
     *            the URL
     * @param requestData
     *            the data to be passed to XMLHttpRequest.send
     * @param contentType
     *            a value for the Content-Type HTTP header
     * @param callback
     *            the callback to notify
     * @return a reference to the sent XmlHttpRequest
     */
    public static XMLHttpRequest post(String url, String requestData,
            String contentType, Callback callback) {
        return request(create(), "POST", url, requestData, contentType,
                callback);
    }

    /**
     * Send a POST request to the <code>url</code> and dispatch updates to the
     * <code>callback</code>.
     *
     * @param window
     *            the window object used to access the XMLHttpRequest
     *            constructor
     * @param url
     *            the URL
     * @param requestData
     *            the data to be passed to XMLHttpRequest.send
     * @param contentType
     *            a value for the Content-Type HTTP header
     * @param callback
     *            the callback to notify
     * @return a reference to the sent XmlHttpRequest
     */
    public static XMLHttpRequest post(Window window, String url,
            String requestData, String contentType, Callback callback) {
        return request(create(window), "POST", url, requestData, contentType,
                callback);
    }

    private static XMLHttpRequest create() {
        return create(Browser.getWindow());
    }

    /**
     * Replacement for {@link XMLHttpRequest#create()} that allows better
     * control of which window object is used to access the XMLHttpRequest
     * constructor.
     */
    private static native XMLHttpRequest create(Window window)
    /*-{
        return new window.XMLHttpRequest();
    }-*/;

    private static XMLHttpRequest request(XMLHttpRequest xhr, String method,
            String url, String requestData, String contentType,
            Callback callback) {
        try {
            xhr.setOnReadyStateChange(new Handler(callback));
            xhr.open(method, url);
            xhr.setRequestHeader("Content-type", contentType);
            xhr.setWithCredentials(true);
            xhr.send(requestData);
        } catch (JavaScriptException e) {
            // Just fail.
            Console.error(e);
            callback.onFail(xhr, e);
            xhr.clearOnReadyStateChange();
        }
        return xhr;
    }

    private static XMLHttpRequest request(XMLHttpRequest xhr, String method,
            String url, final Callback callback) {
        try {
            xhr.setOnReadyStateChange(new Handler(callback));
            xhr.open(method, url);
            xhr.send();
        } catch (JavaScriptException e) {
            // Just fail.
            Console.error(e);
            callback.onFail(xhr, e);
            xhr.clearOnReadyStateChange();
        }
        return xhr;
    }
}
