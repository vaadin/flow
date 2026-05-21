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
package com.vaadin.client.communication;

import jsinterop.annotations.JsType;

import com.google.gwt.xhr.client.XMLHttpRequest;

/**
 * Outcome callbacks delivered to the TypeScript-side {@code Heartbeat}.
 * Implementations dispatch into {@link ConnectionStateHandler}.
 *
 * <p>
 * Exception payload is a string because Java's {@link Exception} doesn't have a
 * stable JS shape; the only consumer reads {@code getMessage()} so the string
 * is sufficient.
 */
@JsType
public interface HeartbeatCallbacks {
    void onOk();

    void onInvalidStatusCode(XMLHttpRequest xhr);

    void onException(XMLHttpRequest xhr, String message);
}
