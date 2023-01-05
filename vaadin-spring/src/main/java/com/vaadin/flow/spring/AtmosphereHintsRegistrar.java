package com.vaadin.flow.spring;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.atmosphere.cache.UUIDBroadcasterCache;
import org.atmosphere.client.TrackMessageSizeInterceptor;
import org.atmosphere.container.JSR356AsyncSupport;
import org.atmosphere.cpr.AtmosphereFramework;
import org.atmosphere.cpr.DefaultAtmosphereResourceFactory;
import org.atmosphere.cpr.DefaultAtmosphereResourceSessionFactory;
import org.atmosphere.cpr.DefaultBroadcaster;
import org.atmosphere.cpr.DefaultBroadcasterFactory;
import org.atmosphere.cpr.DefaultMetaBroadcaster;
import org.atmosphere.interceptor.AtmosphereResourceLifecycleInterceptor;
import org.atmosphere.interceptor.SuspendTrackerInterceptor;
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
        var all = new HashSet<>(Set.of(DefaultAtmosphereResourceFactory.class,
                SimpleHttpProtocol.class,
                AtmosphereResourceLifecycleInterceptor.class,
                TrackMessageSizeInterceptor.class,
                SuspendTrackerInterceptor.class,
                DefaultBroadcasterFactory.class, SimpleBroadcaster.class,
                DefaultBroadcaster.class, UUIDBroadcasterCache.class,
                VoidAnnotationProcessor.class,
                DefaultAtmosphereResourceSessionFactory.class,
                JSR356AsyncSupport.class, DefaultMetaBroadcaster.class));
        all.addAll(AtmosphereFramework.DEFAULT_ATMOSPHERE_INTERCEPTORS);
        return all;
    }

}
