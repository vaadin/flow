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
package com.vaadin.flow.component.trigger.internal;

import java.io.Serializable;

import com.vaadin.flow.dom.Element;
import com.vaadin.flow.dom.JsFunction;
import com.vaadin.flow.js.JsDefinition;
import com.vaadin.flow.js.JsExpression;
import com.vaadin.flow.js.JsFunctionParameterNames;
import com.vaadin.flow.js.JsSnippets;

/**
 * The JavaScript of the built-in trigger inputs, as snippets for
 * {@link JsSnippets#of(Class)}: what {@link Action.Input#toJs(Trigger)}
 * returns. An input is a function that produces a value when it is called: a
 * fixed one, a property of an element, the event or a property of the event it
 * is called with as <code>event</code>, or a value of the trigger context it is
 * called with as <code>context</code>. It is passed to an action, which calls
 * it for the value it works on.
 * <p>
 * For internal use only. May be renamed or removed in a future release.
 */
@JsDefinition
public interface TriggerInputJs extends Serializable {

    /**
     * An input that returns the given value.
     *
     * @param value
     *            the value to return
     * @return the input function
     */
    @JsExpression("return $0")
    JsFunction returnValue(Object value);

    /**
     * An input that returns <code>null</code>, for a slot that is not given an
     * input.
     *
     * @return the input function
     */
    @JsExpression("return null")
    JsFunction returnNull();

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
    JsFunction readProperty(Element target, String propertyName);

    /**
     * An input that reads a property of the event it is called with.
     *
     * @param propertyName
     *            the name of the event property
     * @return the input function
     */
    @JsExpression("return event[$0]")
    @JsFunctionParameterNames("event")
    JsFunction readEventProperty(String propertyName);

    /**
     * An input that returns the event it is called with.
     *
     * @return the input function
     */
    @JsExpression("return event")
    @JsFunctionParameterNames("event")
    JsFunction returnEvent();

    /**
     * An input that reads a property of a member of the trigger context, such
     * as an argument a template called the action with, or <code>null</code>
     * when the trigger supplies no context or the value is missing.
     *
     * @param member
     *            the name of the context member to read from
     * @param property
     *            the name of the property of that member
     * @return the input function
     */
    @JsExpression("return context?.[$0]?.[$1] ?? null")
    @JsFunctionParameterNames({ "event", "context" })
    JsFunction readContextValue(String member, String property);
}
