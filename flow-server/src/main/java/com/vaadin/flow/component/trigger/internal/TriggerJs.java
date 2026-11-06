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

import org.jspecify.annotations.Nullable;

import com.vaadin.flow.dom.Element;
import com.vaadin.flow.dom.JsFunction;
import com.vaadin.flow.internal.nodefeature.ReturnChannelRegistration;
import com.vaadin.flow.js.JsCalledWith;
import com.vaadin.flow.js.JsDefinition;
import com.vaadin.flow.js.JsExpression;
import com.vaadin.flow.js.JsSnippets;

/**
 * The JavaScript of the built-in triggers, actions and inputs, as snippets for
 * {@link JsSnippets#of(Class)}, so that wiring a trigger compiles nothing in
 * the browser.
 * <p>
 * An input is a function that returns its value, and an action is a function
 * that does its work, both called with the event of the trigger as
 * <code>event</code>. An installer is applied to the host element of the
 * trigger and returns the function that uninstalls it, as an initializer of
 * {@link Element#addJsInitializer(String, Object...)} does. Functions are
 * composed by passing one snippet as an argument of another, which calls it.
 * <p>
 * For internal use only. May be renamed or removed in a future release.
 */
@JsDefinition
public interface TriggerJs extends Serializable {

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
     * An input that returns <code>null</code>, for a slot of an action that is
     * not given an input.
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
     * An input that reads a property of the event of the trigger.
     *
     * @param propertyName
     *            the name of the event property
     * @return the input function
     */
    @JsExpression("return event[$0]")
    @JsCalledWith("event")
    JsFunction eventProperty(String propertyName);

    /**
     * An input that returns the event of the trigger itself.
     *
     * @return the input function
     */
    @JsExpression("return event")
    @JsCalledWith("event")
    JsFunction event();

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
     * An action that writes the values of the given inputs to the clipboard,
     * skipping each one that returns <code>null</code>.
     *
     * @param text
     *            the input producing the <code>text/plain</code> payload
     * @param html
     *            the input producing the <code>text/html</code> payload
     * @param image
     *            the input producing the source <code>&lt;img&gt;</code> of the
     *            <code>image/png</code> payload
     * @return the action function, which returns the promise of the write
     */
    @JsExpression("return window.Vaadin.Flow.clipboard.writePayload($0(event), $1(event), $2(event))")
    @JsCalledWith("event")
    JsFunction writeToClipboard(JsFunction text, JsFunction html,
            JsFunction image);

    /**
     * An action that reads the clipboard.
     *
     * @return the action function, which returns the promise of the read
     */
    @JsExpression("return window.Vaadin.Flow.clipboard.readPayload()")
    JsFunction readFromClipboard();

    /**
     * An action that opens the share sheet with the values of the given inputs,
     * leaving out each field whose input is <code>null</code>.
     *
     * @param title
     *            the input producing the title, or <code>null</code>
     * @param text
     *            the input producing the text, or <code>null</code>
     * @param url
     *            the input producing the URL, or <code>null</code>
     * @return the action function, which returns the promise of the share
     */
    @JsExpression("""
            const data = {};
            if ($0) data.title = $0(event);
            if ($1) data.text = $1(event);
            if ($2) data.url = $2(event);
            return navigator.share(data);""")
    @JsCalledWith("event")
    JsFunction share(@Nullable JsFunction title, @Nullable JsFunction text,
            @Nullable JsFunction url);

    /**
     * An action that opens the URL of an input in a new browser tab, unless it
     * is a <code>javascript:</code> URL, so that a URL produced on the client
     * can not run script in the origin of the page. The pattern skips the
     * leading C0 controls and spaces that the URL parser strips before the
     * scheme and ignores case, and <code>String(u)</code> keeps the test from
     * throwing for a value that is not a string.
     *
     * @param url
     *            the input producing the URL
     * @param features
     *            the input producing the features of <code>window.open</code>
     * @return the action function
     */
    @JsExpression("((u) => /^[\\x00-\\x20]*javascript:/i.test(String(u))"
            + " || window.open(u, \"_blank\", $1(event)))($0(event))")
    @JsCalledWith("event")
    JsFunction openInNewTab(JsFunction url, JsFunction features);

    /**
     * An action that downloads from the URL of an input, next to the element
     * the event of the trigger happened on.
     *
     * @param url
     *            the input producing the URL
     * @param filename
     *            the input producing the suggested filename, or
     *            <code>null</code> for none
     * @return the action function
     */
    @JsExpression("window.Vaadin.Flow.download.start($0(event), $1 ? $1(event) : undefined, event)")
    @JsCalledWith("event")
    JsFunction download(JsFunction url, @Nullable JsFunction filename);

    /**
     * An action that requests fullscreen for the whole page.
     *
     * @return the action function, which returns the promise of the request
     */
    @JsExpression("return window.Vaadin.Flow.fullscreen.requestPageFullscreen()")
    JsFunction requestPageFullscreen();

    /**
     * An action that requests fullscreen for an element, moving it into the
     * wrapper element of the UI.
     *
     * @param target
     *            the element to show in fullscreen
     * @param wrapper
     *            the wrapper element of the UI
     * @return the action function, which returns the promise of the request
     */
    @JsExpression("return window.Vaadin.Flow.fullscreen.requestComponentFullscreen($0, $1)")
    JsFunction requestComponentFullscreen(Element target, Element wrapper);

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
