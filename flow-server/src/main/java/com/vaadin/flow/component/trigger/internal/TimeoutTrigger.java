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

import java.time.Duration;
import java.util.Objects;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.dom.JsFunction;
import com.vaadin.flow.shared.Registration;

/**
 * Fires once, on the client, after the given delay has elapsed. The delay is
 * measured by the browser via a single {@code setTimeout} armed when the host
 * is initialized on the client; when it elapses the bound actions run. Pair it
 * with a value-less {@link CallbackAction} (or any other action) to run
 * server-side work a short while later without enabling push.
 * <p>
 * Unlike event- or observer-based triggers, this one fires a single time. The
 * timer is cleared if the trigger is {@link #remove() removed} before it
 * elapses; because it lives in the browser, it also never fires if the host's
 * client DOM goes away (page reload, navigation, …) first.
 * <p>
 * For internal use only. May be renamed or removed in a future release.
 */
public class TimeoutTrigger extends Trigger {

    private final long delayMillis;

    /**
     * Creates a trigger that fires once, the given delay after the host is
     * initialized on the client.
     *
     * @param host
     *            the component whose client-side initialization arms the timer,
     *            not {@code null}
     * @param delay
     *            how long to wait before firing, not {@code null} and not
     *            negative
     */
    public TimeoutTrigger(Component host, Duration delay) {
        super(host);
        Objects.requireNonNull(delay, "delay must not be null");
        if (delay.isNegative()) {
            throw new IllegalArgumentException("delay must not be negative");
        }
        this.delayMillis = delay.toMillis();
    }

    @Override
    protected Registration install(JsFunction action) {
        // Action at $0 (the convention the framework documents in
        // Trigger#install), delay in milliseconds at $1 — both captures of the
        // install JsFunction, no string concatenation around either. The
        // returned function clears the timer when the registration is removed.
        return getHost().addJsInitializer("""
                const id = setTimeout(() => $0(), $1);\
                return () => clearTimeout(id);""", action, delayMillis);
    }
}
