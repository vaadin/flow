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

import com.vaadin.flow.component.internal.ValueJs;
import com.vaadin.flow.dom.Element;
import com.vaadin.flow.dom.JsFunction;
import com.vaadin.flow.internal.nodefeature.ReturnChannelRegistration;
import com.vaadin.flow.js.JsCalledWith;
import com.vaadin.flow.js.JsDefinition;
import com.vaadin.flow.js.JsExpression;
import com.vaadin.flow.js.JsSnippets;

/**
 * The JavaScript that the built-in triggers and actions share, as snippets for
 * {@link JsSnippets#of(Class)}: the actions that report to the server through a
 * return channel or write an element property, and the installers that hook an
 * action up to its trigger in the browser. What a single action does in the
 * browser is declared next to that action, and the inputs that produce a value
 * are in {@link ValueJs}.
 * <p>
 * An action is a function that does its work, called with the event of the
 * trigger as <code>event</code>. An installer is applied to the host element of
 * the trigger and returns the function that uninstalls it, as an initializer of
 * {@link Element#addJsInitializer(String, Object...)} does. Functions are
 * composed by passing one snippet as an argument of another, which calls it.
 * <p>
 * For internal use only. May be renamed or removed in a future release.
 */
@JsDefinition
public interface TriggerJs extends Serializable {

    /**
     * An action that calls a return channel without a value.
     *
     * @param channel
     *            the return channel to call
     * @return the action function
     */
    @JsExpression("$0();")
    JsFunction callChannel(ReturnChannelRegistration channel);

    /**
     * An action that sends the value of an input to a return channel.
     *
     * @param channel
     *            the return channel to call
     * @param source
     *            the input that produces the value
     * @return the action function
     */
    @JsExpression("$0($1(event));")
    @JsCalledWith("event")
    JsFunction sendToChannel(ReturnChannelRegistration channel,
            JsFunction source);

    /**
     * An action that assigns the value of an input to a property of an element.
     *
     * @param target
     *            the element to modify
     * @param propertyName
     *            the name of the property
     * @param source
     *            the input that produces the value
     * @return the action function
     */
    @JsExpression("$0[$1] = $2(event)")
    @JsCalledWith("event")
    JsFunction setProperty(Element target, String propertyName,
            JsFunction source);

    /**
     * An action that runs a promise-returning action and reports the outcome of
     * the promise to a return channel, as <code>{ok, value}</code> when it
     * resolves or as <code>{ok, error: {name, message}}</code> when it rejects.
     *
     * @param promise
     *            the action that returns the promise
     * @param channel
     *            the return channel to report the outcome to
     * @return the action function
     */
    @JsExpression("""
            $0(event).then(value => $1({ok: true, value: value}))\
            .catch(e => $1({ok: false, error: {\
            name: (e && e.name) ? e.name : '',\
            message: (e && e.message) ? e.message : String(e)}}));""")
    @JsCalledWith("event")
    JsFunction reportOutcome(JsFunction promise,
            ReturnChannelRegistration channel);

    /**
     * An installer that runs an action whenever the element receives a DOM
     * event with the given name.
     *
     * @param action
     *            the action to run with the event
     * @param eventName
     *            the name of the DOM event
     * @return the installer function
     */
    @JsExpression("""
            this.addEventListener($1, $0);\
            return () => this.removeEventListener($1, $0);""")
    JsFunction listenToEvent(JsFunction action, String eventName);

    /**
     * An installer that runs an action once, after the given delay.
     *
     * @param action
     *            the action to run
     * @param delayMillis
     *            the delay in milliseconds
     * @return the installer function
     */
    @JsExpression("""
            const id = setTimeout(() => $0(), $1);\
            return () => clearTimeout(id);""")
    JsFunction runAfterTimeout(JsFunction action, long delayMillis);

    /**
     * An installer that runs an action with the size of the element whenever it
     * changes, as <code>{width, height}</code>.
     *
     * @param action
     *            the action to run with the size
     * @return the installer function
     */
    @JsExpression("return window.Vaadin.Flow.elementResize.observe(this, $0);")
    JsFunction observeSize(JsFunction action);
}
