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
package com.vaadin.flow.js;

import java.io.Serializable;

import com.vaadin.flow.dom.Element;
import com.vaadin.flow.dom.JsFunction;

/**
 * JavaScript that is declared on the server and evaluated later on the client,
 * by whatever it is passed to.
 * <p>
 * A method of a {@link JsDefinition} interface that returns a snippet type does
 * not run anything. Called on the implementation that {@link JsSnippets#of}
 * hands out, it answers with a value that names the function the build
 * generated from its {@link JsExpression} and carries the arguments of the
 * call, which the expression references as <code>$0</code>, <code>$1</code>,
 * &hellip;. The value is passed on, for instance as a parameter of
 * {@link Element#executeJs(String, Object...)}, and the client evaluates the
 * expression with those arguments when it decodes the parameter. Nothing is
 * compiled from a string in the browser, so a snippet works under a content
 * security policy that does not allow <code>unsafe-eval</code>.
 * <p>
 * A snippet has no identity on the client and no lifetime. Passing the same
 * snippet to several invocations evaluates its expression once per invocation,
 * and each of them gets a value of its own, so there is nothing to cache and
 * nothing to release.
 * <p>
 * {@link JsFunction} is the snippet type for a function that whoever it is
 * passed to calls with arguments of its own, which {@link JsCalledWith} names.
 *
 * @see JsSnippets#of(Class)
 * @see JsCalledWith
 */
public interface JsSnippet extends Serializable {
}
