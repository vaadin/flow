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
 * @see JsInvokers
 * @see JsSnippets
 * @see JsExpression
 * @see Element#executeJs(Class)
 * @see JsSnippets#of(Class)
 * @since 25.4
 */
@Documented
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface JsDefinition {
}
