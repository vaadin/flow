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
package com.vaadin.flow.component;

import java.io.Serializable;

import tools.jackson.databind.node.ObjectNode;

import com.vaadin.flow.dom.Element;
import com.vaadin.flow.dom.JsExpression;
import com.vaadin.flow.dom.JsInvoker;

/**
 * The client-side operations behind {@link Focusable}, as an invoker interface
 * for {@link Element#getJsInvoker(Class)}.
 * <p>
 * Focus and blur are marked as server-initiated for the client, so that the
 * resulting event reports {@code isFromClient() == false}. A driver of the
 * client side that implements this interface instead of running the scripts is
 * responsible for the same.
 * <p>
 * An invoker interface extends {@link Serializable}, like everything else a
 * component can hold on to.
 */
@JsInvoker
public interface FocusJs extends Serializable {

    /**
     * Focuses the element with browser default options.
     */
    @JsExpression("""
            setTimeout(() => {
                try {
                   this._nextFocusIsFromClient = false;
                   this.focus();
                } finally {
                   this._nextFocusIsFromClient = true;
                }
            }, 0)
            """)
    void focus();

    /**
     * Focuses the element with the given options.
     *
     * @param options
     *            the options of the browser's <code>focus</code> function
     */
    @JsExpression("""
            setTimeout(() => {
                try {
                   this._nextFocusIsFromClient = false;
                   this.focus($0);
                } finally {
                   this._nextFocusIsFromClient = true;
                }
            }, 0)
            """)
    void focus(ObjectNode options);

    /**
     * Removes focus from the element.
     */
    @JsExpression("""
            setTimeout(() => {
                try {
                    this._nextBlurIsFromClient = false;
                    this.blur();
                } finally {
                   this._nextBlurIsFromClient = true;
                }
            }, 0)
            """)
    void blur();
}
