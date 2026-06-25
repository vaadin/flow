/*
 * Copyright (C) 2000-2026 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.spring;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

/**
 * A class for holding development-time cached data. Static fields survive a
 * Spring Boot DevTools reload, so the cached data can be used after reload.
 */
class ReloadCache implements Serializable {
    static Set<Class<?>> lookupClasses;
    static Set<String> validResources = new HashSet<>();
    static Set<String> skippedResources = new HashSet<>();
    static Set<String> dynamicWhiteList;
    static Set<String> routePackages;
    static Set<String> layoutPackages;
    static Set<String> jarClassNames = new HashSet<>();
    static Set<Class<?>> jarClasses = new HashSet<>();
}