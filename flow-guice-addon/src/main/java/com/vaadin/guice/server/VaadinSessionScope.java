package com.vaadin.guice.server;

import com.google.inject.Key;
import com.google.inject.Provider;
import com.google.inject.Scope;

import com.vaadin.server.VaadinSession;

import java.util.HashMap;
import java.util.Map;
import java.util.WeakHashMap;

import static com.google.common.base.Preconditions.checkNotNull;

class VaadinSessionScope implements Scope {

    private final Map<VaadinSession, Map<Key<?>, Object>> scopeMapsBySession = new WeakHashMap<>();

    @Override
    @SuppressWarnings("unchecked")
    public <T> Provider<T> scope(Key<T> key, Provider<T> provider) {
        return () -> {
            final VaadinSession vaadinSession = checkNotNull(VaadinSession.getCurrent());

            final Map<Key<?>, Object> scopeMap = scopeMapsBySession.computeIfAbsent(vaadinSession, v -> new HashMap<>());

            return (T) scopeMap.computeIfAbsent(key, k -> provider.get());
        };
    }
}