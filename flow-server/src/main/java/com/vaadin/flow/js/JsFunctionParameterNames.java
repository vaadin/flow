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

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import com.vaadin.flow.dom.Element;
import com.vaadin.flow.dom.JsFunction;

/**
 * Names the parameters of the JavaScript function that a {@link JsSnippet}
 * evaluates to on the client, so that the {@link JsExpression} can use them.
 * <p>
 * A method returning a {@link JsSnippet} involves two calls with different
 * arguments:
 * <ul>
 * <li>The Java method is called on the server. Its arguments are captured and
 * the expression refers to them as <code>$0</code>, <code>$1</code>, &hellip;,
 * the same way as {@link Element#executeJs(String, Object...)} does.</li>
 * <li>The client turns the snippet into a JavaScript function, and whatever the
 * snippet is passed to calls that function later, in the browser, with
 * arguments of its own. The names listed in this annotation are the parameters
 * of that function, and the expression refers to those arguments by these
 * names.</li>
 * </ul>
 * Declaring a name does not make a value available by itself: the name refers
 * to whatever the client side code calling the function passes in that
 * position. The expression is the body of the function, so it must
 * <code>return</code> a value if the caller expects one.
 * <p>
 * For example, a snippet that shares the page with the Web Share API, where the
 * title, text and URL are computed from the click event by functions given as
 * arguments:
 *
 * <pre>
 * &#64;JsDefinition
 * public interface ShareJs extends Serializable {
 *
 *     &#64;JsExpression("""
 *             const data = {};
 *             if ($0) data.title = $0(event);
 *             if ($1) data.text = $1(event);
 *             if ($2) data.url = $2(event);
 *             return navigator.share(data);""")
 *     &#64;JsFunctionParameterNames("event")
 *     JsFunction share(JsFunction title, JsFunction text, JsFunction url);
 * }
 * </pre>
 *
 * On the client, the snippet works like this function, where <code>$0</code>,
 * <code>$1</code> and <code>$2</code> are the values that
 * <code>share(title, text, url)</code> was called with on the server:
 *
 * <pre>
 * function (event) {
 *   const data = {};
 *   if ($0) data.title = $0(event);
 *   if ($1) data.text = $1(event);
 *   if ($2) data.url = $2(event);
 *   return navigator.share(data);
 * }
 * </pre>
 *
 * The snippet is then passed to code that calls the function, such as a
 * definition that registers it as a click listener. The browser calls a
 * listener with the click event, which the snippet sees as <code>event</code>:
 *
 * <pre>
 * &#64;JsDefinition
 * public interface ClickJs extends Serializable {
 *
 *     &#64;JsExpression("this.addEventListener('click', $0);")
 *     void onClick(JsFunction listener);
 * }
 *
 * ShareJs share = JsSnippets.of(ShareJs.class);
 * button.getElement().executeJs(ClickJs.class).onClick(
 *         share.share(null, null, JsFunction.of("return location.href;")));
 * </pre>
 *
 * Inside the function, <code>this</code> is whatever the caller calls it with.
 * <p>
 * Without this annotation the function takes no parameters, and the expression
 * is its body all the same:
 *
 * <pre>
 * &#64;JsExpression("return import('Frontend/report-editor.js');")
 * JsFunction loadEditor();
 * </pre>
 * <p>
 * A snippet type that is always called in the same way can declare the names
 * once, on the type. The names then apply to all methods that return the type.
 * A method can still declare its own names, and they are used instead. For
 * example, {@link com.vaadin.flow.dom.DomEventFilter} is always called with the
 * DOM <code>event</code> and the <code>element</code> of the listener, so a
 * filter expression can use both:
 *
 * <pre>
 * &#64;JsExpression("return event.key === $0 &amp;&amp; !element.disabled;")
 * DomEventFilter key(String key);
 * </pre>
 * <p>
 * The names are part of the generated JavaScript, so a change to them is a
 * change to the function the client runs, as a change to the expression is.
 *
 * @see JsSnippet
 * @see JsFunction
 * @since 25.4
 */
@Documented
@Target({ ElementType.METHOD, ElementType.TYPE })
@Retention(RetentionPolicy.RUNTIME)
public @interface JsFunctionParameterNames {

    /**
     * The names of the parameters of the function, in the order the caller
     * passes the arguments. Each is a JavaScript identifier, and none of them
     * is <code>$0</code>, <code>$1</code>, &hellip;, which name the captured
     * arguments.
     *
     * @return the parameter names
     */
    String[] value();
}
