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
 * The JavaScript that a method of a JavaScript definition runs, as a constant
 * expression.
 * <p>
 * The annotated method is called through {@link Element#executeJs(Class)}. Its
 * arguments are the parameters of the expression, referenced positionally as
 * <code>$0</code>, <code>$1</code>, &hellip;, and the element the definition
 * was obtained from is <code>this</code> — the same contract as
 * {@link Element#executeJs(String, Object...)}, except that the expression is a
 * constant of the interface instead of a string built at the call site.
 * <p>
 * A method that returns a {@link JsSnippet} is called through
 * {@link JsSnippets#of(Class)} instead, and runs nothing when it is called. Its
 * arguments are still <code>$0</code>, <code>$1</code>, &hellip;, but the
 * expression is evaluated later by what the snippet is passed to, and
 * <code>this</code> is whatever that invoker supplies. See {@link JsCalledWith}
 * for the parameters it is called with.
 *
 * @see Element#executeJs(Class)
 * @see JsSnippets#of(Class)
 * @since 25.4
 */
@Documented
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface JsExpression {

    /**
     * The JavaScript expression to run.
     *
     * @return the expression
     */
    String value();
}
