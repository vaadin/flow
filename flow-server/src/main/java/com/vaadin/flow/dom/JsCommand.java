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
package com.vaadin.flow.dom;

import java.io.Serializable;
import java.util.List;

/**
 * A typed description of a client-side operation, together with the JavaScript
 * that performs it in a browser.
 * <p>
 * A command is scheduled with {@link Element#executeJs(JsCommand)} and travels
 * with the invocation into the pending JavaScript queue of the UI, where
 * {@link com.vaadin.flow.component.internal.UIInternals.JavaScriptInvocation#getCommand()}
 * hands it back. The client receives exactly the expression and the parameters
 * that the equivalent {@link Element#executeJs(String, Object...)} call would
 * send; the command itself never leaves the server.
 * <p>
 * The command exists for a driver of the client side that is not a browser and
 * can not run JavaScript — a browserless test framework, a native bridge. Such
 * a driver takes the queue at a request boundary, in order, and acts on the
 * invocations it recognizes:
 *
 * <pre>
 * for (PendingJavaScriptInvocation pending : internals
 *         .dumpPendingJavaScriptInvocations()) {
 *     switch (pending.getInvocation().getCommand()) {
 *     case FocusCommand focus -&gt; focus(Element.get(pending.getOwner()));
 *     case BlurCommand blur -&gt; blur(Element.get(pending.getOwner()));
 *     case null, default -&gt; recordUnhandledJavaScript(pending);
 *     }
 * }
 * </pre>
 *
 * Without a command, the only thing that identifies an invocation is the text
 * of its expression — and that text is the framework's script wrapped by
 * {@code executeJs}, an implementation detail that a driver would have to match
 * as a substring and that changes silently underneath it.
 * <p>
 * An implementation is a value: immutable, {@link Serializable}, with the
 * arguments of the operation as typed members so that a driver never has to
 * read the generated JavaScript. A record is the natural shape. The target of
 * the operation is not part of the command: it is the element the invocation
 * was scheduled on, available as the owner of the pending invocation.
 *
 * @see Element#executeJs(JsCommand)
 */
public interface JsCommand extends Serializable {

    /**
     * Gets the JavaScript expression that performs this command in a browser.
     * The expression is the one that would be passed to
     * {@link Element#executeJs(String, Object...)}: the element it is scheduled
     * on is available as <code>this</code> and the parameters as
     * <code>$0</code>, <code>$1</code>, &hellip;
     *
     * @return the JavaScript expression, not <code>null</code>
     */
    String getExpression();

    /**
     * Gets the parameters that the expression references positionally.
     *
     * @return the parameters, empty by default, not <code>null</code>
     */
    default List<Object> getParameters() {
        return List.of();
    }
}
