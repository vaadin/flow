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
 * The value of each {@link JsExpression} method is the exact string that is
 * sent, and the method has as many parameters as the string call passes. When
 * the Content Security Policy mode reports string JavaScript, a call such as
 * <code>element.executeJs("return this.scrollTop")</code> then runs the
 * function that the build put in the bundle for
 *
 * <pre>
 * &#64;JsDefinition
 * &#64;JsStringFallback
 * public interface AddonFallbacks {
 *     &#64;JsExpression("return this.scrollTop")
 *     PendingJavaScriptResult scrollTop();
 * }
 * </pre>
 *
 * instead of being refused in the {@code strict} mode or reported in the
 * {@code warn} mode. A string that does not match exactly, including
 * whitespace, is refused or reported as before. In the {@code off} mode the
 * string is sent as it is.
 * <p>
 * The build finds the marked interfaces together with the other
 * {@link JsDefinition} interfaces, and fails if one of them can not stand in
 * for a string call: a method that declares a snippet, is variadic, or declares
 * the same JavaScript with the same parameter count as another marked method.
 * <p>
 * Only {@code Element.executeJs(String, Object...)} and
 * {@code Page.executeJs(String, Object...)} are matched.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface JsStringFallback {
}
