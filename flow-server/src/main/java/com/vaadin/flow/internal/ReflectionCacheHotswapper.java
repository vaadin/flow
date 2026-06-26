/*
 * Copyright (C) 2000-2026 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.internal;

import java.util.Set;

import com.vaadin.flow.hotswap.VaadinHotswapper;
import com.vaadin.flow.server.VaadinService;

/**
 * Clears all mappings from all reflection caches and related resources when one
 * or more classes has been changed.
 */
public class ReflectionCacheHotswapper implements VaadinHotswapper {

    @Override
    public boolean onClassLoadEvent(VaadinService vaadinService,
            Set<Class<?>> classes, boolean redefined) {
        ReflectionCache.clearAll();
        return false;
    }
}
