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

import java.io.Serializable;

import com.vaadin.flow.dom.JsFunction;
import com.vaadin.flow.js.JsDefinition;
import com.vaadin.flow.js.JsExpression;
import com.vaadin.flow.js.JsSnippets;

/**
 * Asks the browser for permission to show notifications when the bound trigger
 * fires.
 * <p>
 * Browsers only show the permission prompt during a user gesture (a click, a
 * key press, …), so bind this action to a {@link Trigger} that fires during
 * such a gesture, typically a {@link ClickTrigger}. The outcome is not reported
 * through this action: the client publishes the new permission to the UI's
 * permission signal, which also covers changes made outside the prompt.
 * <p>
 * For internal use only. May be renamed or removed in a future release.
 */
public class RequestWebNotificationPermissionAction extends Action {

    private static final RequestWebNotificationPermissionJs REQUEST_PERMISSION_JS = JsSnippets
            .of(RequestWebNotificationPermissionJs.class);

    @Override
    protected JsFunction toJs(Trigger trigger) {
        return REQUEST_PERMISSION_JS.requestPermission();
    }

    /**
     * How notification permission is requested, as snippets for
     * {@link JsSnippets#of(Class)}.
     * <p>
     * For internal use only. May be renamed or removed in a future release.
     */
    @JsDefinition
    public interface RequestWebNotificationPermissionJs extends Serializable {

        /**
         * An action that asks the browser for permission to show notifications.
         *
         * @return the action function
         */
        @JsExpression("window.Vaadin.Flow.webNotification.requestPermission()")
        JsFunction requestPermission();
    }
}
