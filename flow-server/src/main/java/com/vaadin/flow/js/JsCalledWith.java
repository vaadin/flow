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
 * The parameters a {@link JsSnippet} is called with by whatever invokes it on
 * the client, which the declared expression references by name.
 * <p>
 * The {@link JsExpression} of the annotated method becomes the body of a
 * function taking these parameters, and that function is the value of the
 * snippet:
 *
 * <pre>
 * &#64;JsDefinition
 * public interface LoggerJs extends Serializable {
 *
 *     &#64;JsExpression("console.log($0, value);")
 *     &#64;JsCalledWith("value")
 *     JsFunction createLogger(String prefix);
 * }
 *
 * &#64;JsDefinition
 * public interface ClickJs extends Serializable {
 *
 *     &#64;JsExpression("this.addEventListener('click', () =&gt; $0('clicked'));")
 *     void onClick(JsFunction handler);
 * }
 *
 * button.getElement().executeJs(ClickJs.class)
 *         .onClick(JsSnippets.of(LoggerJs.class).createLogger("Button:"));
 * </pre>
 * <p>
 * The arguments of the Java call are captured on the server and referenced as
 * <code>$0</code>, <code>$1</code>, &hellip;, the same way as by
 * {@link Element#executeJs(String, Object...)}. The names declared here are
 * what the invoker passes when it calls the function. Inside the function,
 * <code>this</code> is whatever the invoker calls it with.
 * <p>
 * Without this annotation no function is wrapped around the expression, which
 * then has to <code>return</code> the function itself, for an invoker whose
 * calling convention a list of parameters can not express, or for setup that
 * runs once for the function rather than on every call:
 *
 * <pre>
 * &#64;JsExpression("return ({ point }) =&gt; `${point.y} ${$0}`;")
 * JsFunction formatTooltip(String unit);
 * </pre>
 * <p>
 * The client runs such an expression on the first call of the function rather
 * than when it receives the snippet, so it never runs while the parameters of
 * an invocation are decoded, and an expression that does not return a function
 * fails with an error that names the method on that first call. What the
 * invoker gets is a function that forwards to the returned one, so its
 * <code>length</code> is 0.
 * <p>
 * The names are part of the generated JavaScript, so a change to them is a
 * change to the function the client runs, as a change to the expression is.
 *
 * @see JsSnippet
 * @see JsFunction
 */
@Documented
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface JsCalledWith {

    /**
     * The names of the parameters, in the order the invoker passes them. Each
     * is a JavaScript identifier, and none of them is <code>$0</code>,
     * <code>$1</code>, &hellip;, which name the captured arguments.
     *
     * @return the parameter names
     */
    String[] value();
}
