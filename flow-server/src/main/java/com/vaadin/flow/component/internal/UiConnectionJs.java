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
package com.vaadin.flow.component.internal;

import java.io.Serializable;

import com.vaadin.flow.dom.Element;
import com.vaadin.flow.js.JsDefinition;
import com.vaadin.flow.js.JsExpression;

/**
 * What the server tells the wrapper element of a UI about a navigation it was
 * asked for, as a JavaScript definition for {@link Element#executeJs(Class)}.
 * <p>
 * The client hands a navigation to the server and waits to hear what became of
 * it, so every path that ends a server-side navigation answers through one of
 * these.
 * <p>
 * For internal use only. May be renamed or removed in a future release.
 */
@JsDefinition
public interface UiConnectionJs extends Serializable {

    /**
     * Tells the client the server is done with the navigation.
     *
     * @param cancel
     *            <code>true</code> to have the client undo the navigation,
     *            <code>false</code> to have it go through
     */
    @JsExpression("this.serverConnected($0)")
    void serverConnected(boolean cancel);

    /**
     * Tells the client the navigation is postponed, so that it keeps waiting
     * rather than treating the silence as an answer.
     */
    @JsExpression("this.serverPaused()")
    void serverPaused();
}
