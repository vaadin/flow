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
package com.vaadin.client.communication;

import com.google.gwt.xhr.client.XMLHttpRequest;

import elemental.json.JsonObject;

/**
 * XhrConnectionError provides detail about an error which occured during an XHR
 * request to the server
 *
 * @since 7.6
 * @author Vaadin Ltd
 */
public class XhrConnectionError {

    private XMLHttpRequest xhr;
    private JsonObject payload;

    public XhrConnectionError(XMLHttpRequest xhr, JsonObject payload) {
        this.xhr = xhr;
        this.payload = payload;
    }

    /**
     * Returns {@link XMLHttpRequest} which failed to reach the server
     *
     * @return the request which failed
     *
     */
    public XMLHttpRequest getXhr() {
        return xhr;
    }

    /**
     * Returns the payload which was sent to the server
     *
     * @return the payload which was sent, never null
     */
    public JsonObject getPayload() {
        return payload;
    }
}