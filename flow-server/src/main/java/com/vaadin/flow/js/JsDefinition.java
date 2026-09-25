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
 * Marks an interface whose methods declare the JavaScript they run with
 * {@link JsExpression}, to be called through {@link Element#executeJs(Class)}.
 * <p>
 * The annotation is what makes the interface findable during the build: every
 * annotated interface is collected into the generated bundle as a function per
 * method, so the JavaScript an application can invoke from the server is known
 * before it runs and the client never has to build a function from a string.
 * That is what keeps a server-initiated call compatible with a content security
 * policy that does not allow <code>unsafe-eval</code>.
 *
 * @see JsExpression
 * @see Element#executeJs(Class)
 * @since 25.4
 */
@Documented
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface JsDefinition {
}
