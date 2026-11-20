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

/**
 * Marks a {@link JsDefinition} interface whose JavaScript stands in for
 * JavaScript that is sent as a string, typically by an add-on that the
 * application can not change.
 * <p>
 * When the Content Security Policy mode reports string JavaScript, a string
 * that matches what a marked interface declares exactly, including whitespace,
 * runs JavaScript from the bundle instead of being refused in the
 * {@code strict} mode or reported in the {@code warn} mode. A string that does
 * not match is refused or reported as before. In the {@code off} mode strings
 * are sent as they are.
 * <p>
 * What a string is matched against depends on the API that sends it:
 * <ul>
 * <li>{@code Element.executeJs(String, Object...)} and
 * {@code Page.executeJs(String, Object...)}: a method whose
 * {@link JsExpression} value is the string, with as many parameters as the call
 * passes.
 * <li>{@code JsFunction.of(String, Object...)}, and so
 * {@code Element.addJsInitializer(String, Object...)}, and
 * {@code Page.addDynamicImport(String)}: a method that returns a
 * {@code JsFunction}, whose {@link JsExpression} value is the function body,
 * with as many parameters as there are captures. When the code then calls
 * {@code withArguments} on the function, the method declares the same names
 * with {@link JsFunctionParameterNames}; other names send the string again.
 * Methods that return snippets can not be in the same interface as methods that
 * run JavaScript, so these go into an interface of their own.
 * <li>{@code DomListenerRegistration.setFilter(String)} and
 * {@code DomListenerRegistration.addEventData(String)}: the strings listed in
 * {@link #filters()} and {@link #eventData()}.
 * </ul>
 * For example:
 *
 * <pre>
 * &#64;JsDefinition
 * &#64;JsStringFallback(filters = "event.key === 'Escape'")
 * public interface AddonFallbacks {
 *     &#64;JsExpression("return this.scrollTop")
 *     PendingJavaScriptResult scrollTop();
 * }
 *
 * &#64;JsDefinition
 * &#64;JsStringFallback
 * public interface AddonFunctionFallbacks {
 *     &#64;JsExpression("this.title = $0")
 *     JsFunction setTitle(Object title);
 * }
 * </pre>
 * <p>
 * Strings that are built from values that change, for example by joining a
 * color into the JavaScript text, are not supported: a declaration matches one
 * exact string, and the bundle can only hold strings that are known when the
 * application is built. Such a string has to be fixed in the code that sends
 * it, by passing the values as parameters ({@code $0}, {@code $1}, ...) or by
 * using a {@link JsDefinition}. The report for a call site that sends different
 * strings says so.
 * <p>
 * JavaScript that only the browser refuses is not supported either, since Flow
 * does not run it: an inline event handler attribute, such as {@code onclick},
 * or a call to {@code eval} or {@code new Function} in an add-on's own frontend
 * files. The add-on has to change.
 * <p>
 * The JavaScript in the bundle runs in JavaScript strict mode, since the bundle
 * is made of modules, while a string that the browser compiles does not. A
 * string that only works outside strict mode, for example one that assigns to a
 * variable it never declares, fails the same way when it runs from the bundle.
 * Since the declared text has to be the same string, such a string can not be
 * fixed with a fallback: the code that sends it has to change.
 * <p>
 * The build finds the marked interfaces together with the other
 * {@link JsDefinition} interfaces, and fails if one of them can not stand in
 * for a string: a variadic method, a snippet method that does not return a
 * {@code JsFunction}, two methods that declare the same JavaScript with the
 * same parameter count, two methods of one interface with the same name and
 * parameter count, or a filter or event data value that is a property path,
 * which the browser reads without compiling it.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface JsStringFallback {

    /**
     * The filters, as passed to {@code DomListenerRegistration.setFilter}, that
     * the build puts into the bundle as functions, so that setting one of them
     * runs the function instead of compiling the filter.
     *
     * @return the filters, empty by default
     */
    String[] filters() default {};

    /**
     * The event data values, as passed to
     * {@code DomListenerRegistration.addEventData}, that the build puts into
     * the bundle as functions, so that adding one of them runs the function
     * instead of compiling the value. The value is sent under the same key, so
     * reading the event data does not change.
     *
     * @return the event data values, empty by default
     */
    String[] eventData() default {};
}
