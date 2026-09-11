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

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;

import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.server.VaadinService;
import com.vaadin.flow.server.VaadinSession;
import com.vaadin.flow.shared.Registration;

/**
 * Observation hub for {@link Trigger} arming.
 * <p>
 * <strong>Internal test-support API — not for production use.</strong> Its sole
 * purpose is to let a browserless test harness discover which triggers (and
 * actions) are armed on which host, so it can simulate them without a browser.
 * Nothing in production registers or relies on this. It is not part of the
 * public API and may be renamed or removed in any release.
 * <p>
 * The listener registry is scoped to the {@link VaadinService} — it is stored
 * as a {@link com.vaadin.flow.server.VaadinContext} attribute on the service —
 * so a listener registered for one application/service observes only that
 * service's armings, never another's. This keeps concurrent environments (e.g.
 * parallel tests, or multiple Vaadin apps in one JVM) isolated, and leaves
 * nothing behind on a process-global registry once a service is discarded. A
 * JVM with no listener registered creates no registry at all.
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

    /**
     * Per-service registry of arming listeners, held as a
     * {@link com.vaadin.flow.server.VaadinContext} attribute. The list is
     * {@code transient}: the registry is test-only state that should never ride
     * a serialized context, so a deserialized instance simply starts empty.
     */
    private static final class ArmingListeners implements Serializable {

        private transient List<ArmingListener> listeners = new CopyOnWriteArrayList<>();

        private void readObject(ObjectInputStream in)
                throws IOException, ClassNotFoundException {
            in.defaultReadObject();
            listeners = new CopyOnWriteArrayList<>();
        }
    }

    private Triggers() {
    }

    /**
     * Registers a listener notified when triggers are armed or removed within
     * the given service. The listener is stored on the service (as a
     * {@link com.vaadin.flow.server.VaadinContext} attribute), so it observes
     * only armings whose host belongs to {@code service}.
     *
     * @param service
     *            the service whose armings to observe, not {@code null}
     * @param listener
     *            the listener to add, not {@code null}
     * @return a registration that removes the listener when
     *         {@link Registration#remove() removed}
     */
    public static Registration addArmingListener(VaadinService service,
            ArmingListener listener) {
        Objects.requireNonNull(service, "service must not be null");
        Objects.requireNonNull(listener, "listener must not be null");
        service.getContext().getAttribute(ArmingListeners.class,
                ArmingListeners::new).listeners.add(listener);
        return new ArmingRegistration(service, listener);
    }

    /**
     * Registration for an arming listener. The service and listener are held in
     * {@code transient} fields so that, should the registration end up
     * referenced from a serialized {@code VaadinSession}, serialization does
     * not drag in (and possibly fail on) the listener — which belongs to the
     * service's registry, not the session. A deserialized registration refers
     * to nothing, so {@link #remove()} is then a no-op.
     */
    private static final class ArmingRegistration implements Registration {

        private final transient @Nullable VaadinService service;
        private final transient @Nullable ArmingListener listener;

        ArmingRegistration(VaadinService service, ArmingListener listener) {
            this.service = service;
            this.listener = listener;
        }

        @Override
        public void remove() {
            if (service == null || listener == null) {
                return;
            }
            ArmingListeners registry = service.getContext()
                    .getAttribute(ArmingListeners.class);
            if (registry != null) {
                registry.listeners.remove(listener);
            }
        }
    }

    static void notifyArmed(Trigger trigger, List<Action> actions) {
        ArmingListeners registry = registryFor(trigger);
        if (registry == null) {
            return;
        }
        List<Action> copy = List.copyOf(actions);
        for (ArmingListener listener : registry.listeners) {
            try {
                listener.onArmed(trigger, copy);
            } catch (RuntimeException e) {
                LOGGER.error("Trigger arming listener failed", e);
            }
        }
    }

    static void notifyDisarmed(Trigger trigger) {
        ArmingListeners registry = registryFor(trigger);
        if (registry == null) {
            return;
        }
        for (ArmingListener listener : registry.listeners) {
            try {
                listener.onDisarmed(trigger);
            } catch (RuntimeException e) {
                LOGGER.error("Trigger arming listener failed", e);
            }
        }
    }

    private static @Nullable ArmingListeners registryFor(Trigger trigger) {
        VaadinService service = serviceOf(trigger);
        // No default supplier: absence means no listener was ever registered
        // for this service, so there is nothing to notify.
        return service == null ? null
                : service.getContext().getAttribute(ArmingListeners.class);
    }

    private static @Nullable VaadinService serviceOf(Trigger trigger) {
        // Prefer the host's own service, which is authoritative when the host
        // is
        // attached. Fall back to the current service — what a browserless
        // harness has at arm time, since a trigger is typically armed during
        // view construction, before the host is attached to a UI.
        VaadinService hostService = trigger.getHost().getComponent()
                .flatMap(Component::getUI).map(UI::getSession)
                .map(VaadinSession::getService).orElse(null);
        return hostService != null ? hostService : VaadinService.getCurrent();
    }
}
