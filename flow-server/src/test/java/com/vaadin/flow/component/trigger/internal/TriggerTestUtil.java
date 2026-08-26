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

import java.util.List;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.internal.PendingJavaScriptInvocation;
import com.vaadin.flow.component.internal.UIInternals.JavaScriptInvocation;
import com.vaadin.flow.dom.JsFunction;
import com.vaadin.flow.internal.nodefeature.ReturnChannelRegistration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Shared test helpers for navigating the {@link JsFunction} tree produced by
 * {@link Trigger}s and {@link Action}s. Each call dumps the UI's pending JS
 * invocations and asserts the expected shape, so tests can focus on the
 * relevant rendered fragments instead of repeating the same plumbing.
 */
final class TriggerTestUtil {

    private TriggerTestUtil() {
    }

    /**
     * Returns every pending install {@link JsFunction} on the UI, in the order
     * the triggers registered them.
     */
    static List<JsFunction> installFns(UI ui) {
        return ui.getInternals().dumpPendingJavaScriptInvocations().stream()
                .map(PendingJavaScriptInvocation::getInvocation)
                .map(TriggerTestUtil::installFn).toList();
    }

    /**
     * Asserts that exactly one install {@link JsFunction} is pending on the UI
     * and returns it.
     */
    static JsFunction singleInstallFn(UI ui) {
        List<JsFunction> installs = installFns(ui);
        assertEquals(1, installs.size(), "Expected exactly one pending JS");
        return installs.get(0);
    }

    /**
     * Extracts the install {@link JsFunction} from an {@code addJsInitializer}
     * invocation. Parameters are {@code [element, initializerId, installFn]};
     * the installFn is the trigger's install {@link JsFunction}.
     */
    static JsFunction installFn(JavaScriptInvocation invocation) {
        Object o = invocation.getParameters().get(2);
        assertTrue(o instanceof JsFunction, "Expected $2 to be a JsFunction");
        return (JsFunction) o;
    }

    /**
     * Returns the action {@link JsFunction} captured at install {@code $0}.
     * With the per-action install model, that capture is the action
     * {@link JsFunction} directly — no intermediate composed-handler layer.
     */
    static JsFunction actionOf(JsFunction installFn) {
        Object o = installFn.getCaptures().get(0);
        assertTrue(o instanceof JsFunction,
                "Expected install $0 to be the action JsFunction");
        return (JsFunction) o;
    }

    /**
     * Asserts that the single pending install's action {@link JsFunction}
     * captures exactly one {@link ReturnChannelRegistration} and returns it.
     */
    static ReturnChannelRegistration singleReturnChannel(UI ui) {
        JsFunction action = actionOf(singleInstallFn(ui));
        List<ReturnChannelRegistration> channels = action.getCaptures().stream()
                .filter(o -> o instanceof ReturnChannelRegistration)
                .map(o -> (ReturnChannelRegistration) o).toList();
        assertEquals(1, channels.size(),
                "Expected exactly one captured return channel");
        return channels.get(0);
    }
}
