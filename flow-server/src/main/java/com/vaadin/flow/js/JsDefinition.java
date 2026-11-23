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

import com.vaadin.flow.component.page.JavaScriptResult;
import com.vaadin.flow.component.page.Page;
import com.vaadin.flow.component.page.PendingJavaScriptResult;
import com.vaadin.flow.dom.DomEventData;
import com.vaadin.flow.dom.DomEventFilter;
import com.vaadin.flow.dom.Element;
import com.vaadin.flow.dom.JsFunction;

/**
 * Marks an interface that declares JavaScript, so that the build and the
 * development server find it. Every annotated interface is collected into the
 * generated bundle as a function per method, so the JavaScript an application
 * can invoke from the server is known before it runs and the client never has
 * to build a function from a string. That is what keeps a server-initiated call
 * compatible with a content security policy that does not allow
 * <code>unsafe-eval</code>.
 * <p>
 * What the interface declares is up to the marker interface it extends: an
 * interface that extends {@link JsInvokers} declares, with
 * {@link JsExpression}, JavaScript that runs when a method is called through
 * {@link Element#executeJs(Class)}, and one that extends {@link JsSnippets}
 * declares {@link JsSnippet snippets}, obtained through
 * {@link JsSnippets#of(Class)}. An interface that is used through either of
 * them without this annotation is refused, since its JavaScript would be
 * missing from the bundle.
 * <p>
 * The annotation, rather than the marker interface, is what the interface is
 * found by, since a servlet container is not required to report the interfaces
 * that extend a type listed in <code>&#64;HandlesTypes</code>, while it does
 * report the types annotated with one listed there.
 *
 * <pre>
 * &#64;JsDefinition
 * public interface ScrollJs extends JsInvokers {
 *
 *     &#64;JsExpression("this.scrollTop = $0;")
 *     void scrollTo(int top);
 * }
 * </pre>
 *
 * <h2>Which marker to extend</h2>
 *
 * Extend {@link JsInvokers} when the server wants JavaScript to run now,
 * optionally with a result. Its methods return <code>void</code>,
 * {@link JavaScriptResult JavaScriptResult&lt;T&gt;} or
 * {@link PendingJavaScriptResult}. For an implementation obtained from
 * {@link Element#executeJs(Class)}, <code>this</code> is the element; one
 * obtained from {@link Page#executeJs(Class)} runs on nothing in particular.
 * <p>
 * Extend {@link JsSnippets} when you need a function to pass on: as an argument
 * of an invoker method, as a {@link DomEventFilter} or {@link DomEventData} of
 * a listener, or as a snippet type that a component accepts. Its methods return
 * {@link JsFunction}, {@link JsSnippet}, {@link DomEventFilter},
 * {@link DomEventData} or an interface that extends {@link JsSnippet}. They run
 * nothing when called, and <code>this</code> is whatever calls the function.
 * Implementations are obtained from {@link JsSnippets#of(Class)}.
 * <p>
 * One interface can not do both. Declare the two kinds in two interfaces, as in
 * the example of {@link JsSnippets}. {@link JsFunctionParameterNames} applies
 * only to the methods of a {@link JsSnippets} interface.
 *
 * <h2>Inheritance</h2>
 *
 * A definition can extend other interfaces, and the methods it inherits are
 * part of it: they are collected into the bundle with the definition and can be
 * called through its implementation, as if the definition declared them itself.
 * <p>
 * Annotations on interfaces are not inherited, so every interface that is
 * passed to {@link Element#executeJs(Class)}, {@link Page#executeJs(Class)} or
 * {@link JsSnippets#of(Class)} needs its own {@code @JsDefinition}, even when
 * an interface it extends has one. An interface that is only extended, and
 * never passed to any of them itself, does not need the annotation:
 *
 * <pre>
 * // Only extended, so not annotated
 * public interface ScrollableJs extends JsInvokers {
 *
 *     &#64;JsExpression("this.scrollTop = $0;")
 *     void scrollTo(int top);
 * }
 *
 * &#64;JsDefinition
 * public interface ListJs extends ScrollableJs {
 *
 *     &#64;JsExpression("this.scrollTop = this.scrollHeight;")
 *     void scrollToEnd();
 * }
 *
 * element.executeJs(ListJs.class).scrollTo(100);
 * </pre>
 *
 * A sub-interface can override an inherited method with another
 * {@link JsExpression}. The JavaScript of the override is what runs for that
 * method when it is called through the sub-interface; the inherited one still
 * runs through the interface that declares it.
 *
 * @see JsInvokers
 * @see JsSnippets
 * @see JsExpression
 * @see Element#executeJs(Class)
 * @see Page#executeJs(Class)
 * @see JsSnippets#of(Class)
 * @since 25.4
 */
@Documented
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface JsDefinition {
}
