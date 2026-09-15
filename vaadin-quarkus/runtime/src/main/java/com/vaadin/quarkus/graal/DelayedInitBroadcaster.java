/*
 * Copyright 2000-2025 Vaadin Ltd.
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
package com.vaadin.quarkus.graal;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

import org.atmosphere.cpr.AtmosphereConfig;
import org.atmosphere.cpr.AtmosphereFramework;
import org.atmosphere.cpr.BroadcasterConfig;
import org.atmosphere.cpr.DefaultBroadcaster;

/**
 * A broadcaster implementation that postpone schedulers initialization.
 * <p>
 * </p>
 * Usually initialization is performed during Atmosphere init() call, but this
 * prevents a native build to complete because starting threads at build time is
 * not supported. Postponed initialization is activated by a call to
 * {@link #startExecutors(AtmosphereFramework)}.
 */
public class DelayedInitBroadcaster extends DefaultBroadcaster {

    private final AtomicBoolean executorsInitialized = new AtomicBoolean(false);

    @Override
    protected BroadcasterConfig createBroadcasterConfig(
            AtmosphereConfig config) {
        return new DelayedInitBroadcasterConfig(
                config.framework().broadcasterFilters(), config, getID())
                .init();
    }

    @Override
    protected void spawnReactor() {
        if (executorsInitialized.get()) {
            super.spawnReactor();
        }
    }

    @Override
    protected void start() {
        if (!started.getAndSet(true)) {
            if (executorsInitialized.get()) {
                super.start();
            }
        }
    }

    void delayedInit() {
        if (getBroadcasterConfig() instanceof DelayedInitBroadcasterConfig cfg) {
            if (executorsInitialized.compareAndSet(false, true)) {
                cfg.configExecutors();
                if (started.get()) {
                    super.start();
                }
            }
        }
    }

    static void startExecutors(AtmosphereFramework framework) {
        if (framework != null) {
            framework.getAtmosphereHandlers().values().stream()
                    .map(h -> h.broadcaster).filter(Objects::nonNull)
                    .filter(b -> b instanceof DelayedInitBroadcaster)
                    .map(DelayedInitBroadcaster.class::cast)
                    .forEach(DelayedInitBroadcaster::delayedInit);
        }
    }

    private static class DelayedInitBroadcasterConfig
            extends BroadcasterConfig {

        public DelayedInitBroadcasterConfig(List<String> broadcastFilters,
                AtmosphereConfig config, String broadcasterId) {
            super(broadcastFilters, config, false, broadcasterId);
        }

        @Override
        protected synchronized void configExecutors() {
            super.configExecutors();
        }
    }
}
