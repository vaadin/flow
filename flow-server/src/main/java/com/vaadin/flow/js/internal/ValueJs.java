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
package com.vaadin.flow.js.internal;

import java.io.Serializable;

import com.vaadin.flow.dom.Element;
import com.vaadin.flow.dom.JsFunction;
import com.vaadin.flow.js.JsCalledWith;
import com.vaadin.flow.js.JsDefinition;
import com.vaadin.flow.js.JsExpression;
import com.vaadin.flow.js.JsSnippets;

/**
 * Snippets for {@link JsSnippets#of(Class)} that produce a value when they are
 * called: a fixed one, a property of an element, or the event or a property of
 * the event they are called with as <code>event</code>. They are meant to be
 * passed to another snippet that calls them for the value it works on.
 * <p>
 * For internal use only. May be renamed or removed in a future release.
 */
@JsDefinition
public interface ValueJs extends Serializable {

    /**
     * An input that returns the given value.
     *
     * @param value
     *            the value to return
     * @return the input function
     */
    @JsExpression("return $0")
    JsFunction value(Object value);

    /**
     * An input that returns <code>null</code>, for a slot that is not given an
     * input.
     *
     * @return the input function
     */
    @JsExpression("return null")
    JsFunction nullValue();

    /**
     * An input that reads a property of an element when it is called.
     *
     * @param target
     *            the element to read from
     * @param propertyName
     *            the name of the property
     * @return the input function
     */
    @JsExpression("return $0[$1]")
    JsFunction property(Element target, String propertyName);

    /**
     * An input that reads a property of the event it is called with.
     *
     * @param propertyName
     *            the name of the event property
     * @return the input function
     */
    @JsExpression("return event[$0]")
    @JsCalledWith("event")
    JsFunction eventProperty(String propertyName);

    /**
     * An input that returns the event it is called with.
     *
     * @return the input function
     */
    @JsExpression("return event")
    @JsCalledWith("event")
    JsFunction event();
}
