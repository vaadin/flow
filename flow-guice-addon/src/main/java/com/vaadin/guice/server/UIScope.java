package com.vaadin.guice.server;

import com.google.inject.Key;
import com.google.inject.Provider;
import com.google.inject.Scope;
import com.vaadin.server.VaadinSession;
import com.vaadin.ui.UI;

import java.util.HashMap;
import java.util.Map;
import java.util.WeakHashMap;

import static com.google.common.base.Preconditions.checkNotNull;

class UIScope implements Scope {

    private final Map<VaadinSession, Map<UI, Map<Key<?>, Object>>> scopesBySession = new WeakHashMap<>();

    @Override
    @SuppressWarnings("unchecked")
    public <T> Provider<T> scope(Key<T> key, Provider<T> provider) {
        return () -> {

            final VaadinSession vaadinSession = checkNotNull(
                VaadinSession.getCurrent(),
                "VaadinSession is not set up yet."
            );

            final UI currentUI = checkNotNull(
                UI.getCurrent(),
                "current UI is not set up yet"
            );

            Map<UI, Map<Key<?>, Object>> uisToScopedObjects = scopesBySession.computeIfAbsent(vaadinSession, session -> new WeakHashMap<>());

            final Map<Key<?>, Object> scopedObjects = uisToScopedObjects.computeIfAbsent(currentUI, ui -> new HashMap<>());

            return (T) scopedObjects.computeIfAbsent(key, k -> provider.get());
        };
    }
}
