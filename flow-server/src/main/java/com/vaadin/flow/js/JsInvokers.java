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

import com.vaadin.flow.component.page.Page;
import com.vaadin.flow.dom.Element;

/**
 * Marks an interface whose methods declare, with {@link JsExpression}, the
 * JavaScript they run when they are called. A method without the annotation
 * calls the method of the same name on the element. An implementation is
 * obtained from {@link Element#executeJs(Class)} or
 * {@link Page#executeJs(Class)}, and calling a method of it schedules the
 * declared JavaScript to run on the client.
 * <p>
 * A call through {@link Page#executeJs(Class)} runs on no element, so it can
 * not call a method on one. An interface that has a method without
 * {@link JsExpression} that calls the method of the same name on the element,
 * declared in the interface itself or inherited from another interface that
 * extends {@link JsInvokers}, can only be used with
 * {@link Element#executeJs(Class)}: {@link Page#executeJs(Class)} throws an
 * {@link IllegalArgumentException} for it. See
 * {@link JsCall#declaredExpression(java.lang.reflect.Method)} for which methods
 * call the element method.
 * <p>
 * The interface is also annotated with {@link JsDefinition}, which is what
 * makes it findable during the build: its JavaScript is collected into the
 * generated bundle, so the client never has to build a function from a string.
 * That is what keeps a server-initiated call compatible with a content security
 * policy that does not allow <code>unsafe-eval</code>. See {@link JsDefinition}
 * for how a definition that extends another interface is declared.
 * <p>
 * An interface whose methods only answer with {@link JsSnippet snippets}
 * extends {@link JsSnippets} instead. An interface can not extend both. Both
 * are {@link Serializable}, so an implementation can be kept in a field of a
 * component.
 *
 * <pre>
 * &#64;JsDefinition
 * public interface ScrollJs extends JsInvokers {
 *
 *     &#64;JsExpression("this.scrollTop = $0;")
 *     void scrollTo(int top);
 *
 *     // Runs this.scrollIntoView($0) on the element
 *     void scrollIntoView(JsonNode options);
 * }
 *
 * element.executeJs(ScrollJs.class).scrollTo(100);
 * </pre>
 *
 * @see JsDefinition
 * @see JsExpression
 * @see Element#executeJs(Class)
 * @see Page#executeJs(Class)
 * @see JsSnippets
 */
public interface JsInvokers extends Serializable {
}
