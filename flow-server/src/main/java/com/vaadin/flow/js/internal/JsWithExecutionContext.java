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

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import com.vaadin.flow.js.JsExpression;

/**
 * Marks a method declaring {@link JsExpression JavaScript} that needs the
 * client-side execution context of the application, which an expression passed
 * to {@code executeJs(String, Object...)} runs against as <code>this</code>.
 * <p>
 * The generated function takes the context as a leading parameter named
 * <code>$executionContext</code>, followed by the arguments of the call as
 * <code>$0</code>, <code>$1</code>, &hellip;, and <code>this</code> stays the
 * element the definition was obtained from. The context carries the framework
 * callbacks, such as <code>stopApplication()</code> and
 * <code>attachExistingElement(parent, previousSibling, tagName, id)</code>,
 * which a function of the bundle otherwise has no way to reach.
 * <p>
 * Whether a function gets the context is decided by the bundle: the build
 * registers a marked method apart from the others, so a message from the server
 * can not ask for the context to be given to any other function.
 * <p>
 * For internal use only. The context exposes the internals of the client, which
 * are not an API. May be renamed or removed in any release.
 */
@Documented
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface JsWithExecutionContext {
}
