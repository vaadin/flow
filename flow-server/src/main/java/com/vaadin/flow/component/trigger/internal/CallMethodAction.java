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
package com.vaadin.flow.component.trigger.internal;

import java.util.Objects;

import org.jspecify.annotations.Nullable;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.dom.Element;
import com.vaadin.flow.dom.JsFunction;

/**
 * Calls a JavaScript method on a target element when the bound trigger fires.
 * Pure client-side — no server round-trip; the method's return value is
 * discarded.
 * <p>
 * Each argument is supplied as an {@link Action.Input}: a literal captured at
 * build time, the current value of a DOM property, an event-scoped expression,
 * or any other input. The rendered JS evaluates {@code target[method](arg0,
 * arg1, ...)} where each {@code argN} is the input's value at the moment the
 * trigger fires.
 * <p>
 * Common idioms:
 * <ul>
 * <li>Focus an input on click:
 * {@code new CallMethodAction(input, "focus")}</li>
 * <li>Scroll an element into view:
 * {@code new CallMethodAction(panel, "scrollIntoView")}</li>
 * <li>Select all text: {@code new CallMethodAction(field, "select")}</li>
 * <li>Click another element: {@code new CallMethodAction(other, "click")}</li>
 * <li>Submit a form: {@code new CallMethodAction(form, "requestSubmit")}</li>
 * <li>Play / pause a media element: {@code new CallMethodAction(video, "play")}
 * / {@code new CallMethodAction(video, "pause")}</li>
 * <li>Pass an options object built on the server:
 * {@code new CallMethodAction(panel, "scrollIntoView",
 * new LiteralInput<>(Map.of("behavior", "smooth")))}</li>
 * </ul>
 * <p>
 * For methods that return a {@code Promise}, the promise is created and dropped
 * — the JS engine may log an unhandled-rejection warning if the promise
 * rejects. Use {@link PromiseAction} based actions when you need to observe the
 * outcome on the server.
 * <p>
 * Server-side state is not updated by this action; the call lives in the
 * browser until the next sync from the client (if any).
 * <p>
 * For internal use only. May be renamed or removed in a future release.
 */
public class CallMethodAction extends Action {

    private final Element target;
    private final String methodName;
    private final Action.Input<?>[] arguments;

    /**
     * Creates an action that, when the trigger fires, evaluates each input on
     * the client and calls {@code target[methodName](...)} with those values.
     *
     * @param target
     *            the component on whose root element the method is invoked, not
     *            {@code null}
     * @param methodName
     *            the JS method name (e.g. {@code "focus"},
     *            {@code "scrollIntoView"}), not {@code null}
     * @param arguments
     *            inputs producing the method's positional arguments, in order;
     *            may be empty for a no-arg call. Neither the array nor any
     *            element may be {@code null}
     */
    public CallMethodAction(Component target, String methodName,
            Action.Input<?>... arguments) {
        this.target = Objects.requireNonNull(target, "target must not be null")
                .getElement();
        this.methodName = Objects.requireNonNull(methodName,
                "methodName must not be null");
        Objects.requireNonNull(arguments, "arguments must not be null");
        Action.Input<?>[] copy = arguments.clone();
        for (Action.@Nullable Input<?> arg : copy) {
            Objects.requireNonNull(arg, "argument inputs must not be null");
        }
        this.arguments = copy;
    }

    @Override
    protected JsFunction toJs(Trigger trigger) {
        // The body assembled here contains only structural JS tokens —
        // positional capture references ($0, $1, ...), parens, commas, and
        // the literal word "event". No value, name, or other piece of caller
        // data is ever interpolated into the string: target is captured at
        // $0, methodName at $1, and each argument's evaluator JsFunction at
        // $2 onwards. The shape varies with the number of arguments, which
        // is what makes the body assembly itself dynamic, but the contents
        // do not.
        Object[] captures = new Object[2 + arguments.length];
        captures[0] = target;
        captures[1] = methodName;
        StringBuilder body = new StringBuilder("$0[$1](");
        for (int i = 0; i < arguments.length; i++) {
            if (i > 0) {
                body.append(", ");
            }
            body.append('$').append(i + 2).append("(event)");
            captures[i + 2] = arguments[i].toJs(trigger);
        }
        body.append(')');
        return JsFunction.of(body.toString(), captures).withArguments("event");
    }
}
