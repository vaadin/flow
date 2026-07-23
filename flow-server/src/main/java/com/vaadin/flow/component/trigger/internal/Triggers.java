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
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;

import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vaadin.flow.shared.Registration;

/**
 * Cross-cutting operations over {@link Trigger}s.
 * <p>
 * Currently a hub for observing trigger arming: an {@link ArmingListener} is
 * notified whenever a trigger is armed with actions or removed. A JVM with no
 * listener registered pays only an empty-list check per arm, so this is
 * effectively free in production. The intended consumer is a test harness that
 * needs to discover which triggers (and actions) are wired to which host
 * without a browser.
 * <p>
 * Listeners are held in a static list, so they are never part of a
 * {@code VaadinSession}'s serialized state.
 * <p>
 * For internal use only. May be renamed or removed in a future release.
 *
 * @since 25.2
 */
public final class Triggers {

    private static final Logger LOGGER = LoggerFactory
            .getLogger(Triggers.class);

    /**
     * Notified when a {@link Trigger} is armed with actions or removed.
     */
    public interface ArmingListener {

        /**
         * Invoked when {@link Trigger#triggers(Action...)} commits actions to a
         * trigger. Fired once per {@code triggers} call with that call's
         * actions, in order; a trigger armed by several calls produces several
         * notifications.
         *
         * @param trigger
         *            the trigger that was armed, not {@code null}
         * @param actions
         *            the actions committed in this call, in order, not
         *            {@code null}
         */
        void onArmed(Trigger trigger, List<Action> actions);

        /**
         * Invoked when {@link Trigger#remove()} detaches a trigger. May fire
         * for a trigger that was never armed; consumers should ignore triggers
         * they have not recorded.
         *
         * @param trigger
         *            the trigger that was removed, not {@code null}
         */
        default void onDisarmed(Trigger trigger) {
        }
    }

    // Static => never serialized with the session; empty in production.
    private static final List<ArmingListener> LISTENERS = new CopyOnWriteArrayList<>();

    private Triggers() {
    }

    /**
     * Registers a listener notified when triggers are armed or removed.
     *
     * @param listener
     *            the listener to add, not {@code null}
     * @return a registration that removes the listener when
     *         {@link Registration#remove() removed}
     */
    public static Registration addArmingListener(ArmingListener listener) {
        Objects.requireNonNull(listener, "listener must not be null");
        LISTENERS.add(listener);
        return new ArmingRegistration(listener);
    }

    /**
     * Registration for an arming listener. The listener is held in a
     * {@code transient} field so that, should the registration end up
     * referenced from a serialized {@code VaadinSession}, serialization does
     * not drag in (and possibly fail on) the listener — which belongs to the
     * JVM-static listener list, not the session. A deserialized registration
     * refers to no live listener, so {@link #remove()} is then a no-op.
     */
    private static final class ArmingRegistration implements Registration {

        private final transient @Nullable ArmingListener listener;

        ArmingRegistration(ArmingListener listener) {
            this.listener = listener;
        }

        @Override
        public void remove() {
            if (listener != null) {
                LISTENERS.remove(listener);
            }
        }
    }

    static void notifyArmed(Trigger trigger, List<Action> actions) {
        if (LISTENERS.isEmpty()) {
            return;
        }
        List<Action> copy = List.copyOf(actions);
        for (ArmingListener listener : LISTENERS) {
            try {
                listener.onArmed(trigger, copy);
            } catch (RuntimeException e) {
                LOGGER.error("Trigger arming listener failed", e);
            }
        }
    }

    static void notifyDisarmed(Trigger trigger) {
        if (LISTENERS.isEmpty()) {
            return;
        }
        for (ArmingListener listener : LISTENERS) {
            try {
                listener.onDisarmed(trigger);
            } catch (RuntimeException e) {
                LOGGER.error("Trigger arming listener failed", e);
            }
        }
    }
}
