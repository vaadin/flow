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
package com.vaadin.flow.spring.springnative;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.atmosphere.cache.UUIDBroadcasterCache;
import org.atmosphere.client.TrackMessageSizeInterceptor;
import org.atmosphere.config.managed.ManagedServiceInterceptor;
import org.atmosphere.config.service.AtmosphereHandlerService;
import org.atmosphere.container.JSR356AsyncSupport;
import org.atmosphere.cpr.AsyncSupportListener;
import org.atmosphere.cpr.AsyncSupportListenerAdapter;
import org.atmosphere.cpr.AtmosphereFramework;
import org.atmosphere.cpr.AtmosphereFrameworkListener;
import org.atmosphere.cpr.AtmosphereInterceptor;
import org.atmosphere.cpr.AtmosphereResource;
import org.atmosphere.cpr.AtmosphereResourceEventListener;
import org.atmosphere.cpr.AtmosphereResourceImpl;
import org.atmosphere.cpr.BroadcastFilter;
import org.atmosphere.cpr.DefaultAnnotationProcessor;
import org.atmosphere.cpr.DefaultAtmosphereResourceFactory;
import org.atmosphere.cpr.DefaultAtmosphereResourceSessionFactory;
import org.atmosphere.cpr.DefaultBroadcaster;
import org.atmosphere.cpr.DefaultBroadcasterFactory;
import org.atmosphere.cpr.DefaultMetaBroadcaster;
import org.atmosphere.interceptor.AtmosphereResourceLifecycleInterceptor;
import org.atmosphere.interceptor.SuspendTrackerInterceptor;
import org.atmosphere.util.AbstractBroadcasterProxy;
import org.atmosphere.util.ExcludeSessionBroadcaster;
import org.atmosphere.util.SimpleBroadcaster;
import org.atmosphere.util.VoidAnnotationProcessor;
import org.atmosphere.websocket.protocol.SimpleHttpProtocol;
import org.springframework.aot.hint.MemberCategory;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.RuntimeHintsRegistrar;
import org.springframework.core.io.ClassPathResource;

/**
 * Registers runtime hints for Spring 3 native support for Atmosphere.
 * <p>
 * These should go into Atmosphere, see
 * https://github.com/Atmosphere/atmosphere/issues/2483
 */
class AtmosphereHintsRegistrar implements RuntimeHintsRegistrar {

    @Override
    public void registerHints(RuntimeHints hints, ClassLoader classLoader) {
        hints.resources().registerResource(new ClassPathResource(
                "org/atmosphere/util/version.properties"));
        var reflectionHints = hints.reflection();

        for (Class<?> c : getAtmosphereClasses()) {
            reflectionHints.registerType(c, MemberCategory.values());
        }
    }

    private Collection<? extends Class<?>> getAtmosphereClasses() {
        var all = new HashSet<>(Set.of(AsyncSupportListenerAdapter.class,
                AtmosphereFramework.class, DefaultAnnotationProcessor.class,
                DefaultAtmosphereResourceFactory.class,
                SimpleHttpProtocol.class,
                AtmosphereResourceLifecycleInterceptor.class,
                TrackMessageSizeInterceptor.class,
                SuspendTrackerInterceptor.class,
                DefaultBroadcasterFactory.class, SimpleBroadcaster.class,
                DefaultBroadcaster.class, UUIDBroadcasterCache.class,
                VoidAnnotationProcessor.class,
                DefaultAtmosphereResourceSessionFactory.class,
                JSR356AsyncSupport.class, DefaultMetaBroadcaster.class,
                AtmosphereHandlerService.class, AbstractBroadcasterProxy.class,
                AsyncSupportListener.class, AtmosphereFrameworkListener.class,
                ExcludeSessionBroadcaster.class,
                AtmosphereResourceEventListener.class,
                AtmosphereInterceptor.class, BroadcastFilter.class,
                AtmosphereResource.class, AtmosphereResourceImpl.class,
                ManagedServiceInterceptor.class));
        all.addAll(AtmosphereFramework.DEFAULT_ATMOSPHERE_INTERCEPTORS);
        return all;
    }

}
