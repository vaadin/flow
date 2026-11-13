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
 * For example, a snippet for a click listener on a link that shares the link
 * with the Web Share API instead of following it. The title and the text are
 * given on the server, and the URL is read from the clicked link through the
 * click <code>event</code>:
 *
 * <pre>
 * &#64;JsDefinition
 * public interface ShareJs extends Serializable {
 *
 *     &#64;JsExpression("""
 *             event.preventDefault();
 *             return navigator.share({
 *                 title: $0, text: $1, url: event.currentTarget.href });""")
 *     &#64;JsFunctionParameterNames("event")
 *     JsFunction share(String title, String text);
 * }
 * </pre>
 *
 * The build generates the following JavaScript for <code>share</code>. The
 * outer function takes the arguments of the Java call as its parameters
 * <code>$0</code> and <code>$1</code>. The client calls it once, with the
 * values sent from the server, when it receives the snippet. The inner function
 * is what the snippet evaluates to. Its parameters are the names of this
 * annotation, its body is the expression, and it sees <code>$0</code> and
 * <code>$1</code> through the closure:
 *
 * <pre>
 * function ($0, $1) {
 *   return function (event) {
 *     event.preventDefault();
 *     return navigator.share({
 *         title: $0, text: $1, url: event.currentTarget.href });
 *   };
 * }
 * </pre>
 *
 * The snippet is then passed to code that calls the inner function, such as a
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
 * Anchor link = new Anchor("/reports/q3", "Share the report");
 * link.getElement().executeJs(ClickJs.class).onClick(JsSnippets
 *         .of(ShareJs.class).share("Quarterly report", "Have a look"));
 * </pre>
 *
 * All of the JavaScript is declared in the definitions and generated into the
 * bundle, so nothing is compiled from a string in the browser.
 * <p>
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
