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
import java.util.stream.Collectors;

/**
 * This event is fired by {@link ReloadListener} when Spring Boot DevTools is
 * about to trigger a reload.
 */
class ReloadEvent implements Serializable {

    private final Set<String> addedClasses = new HashSet<>();
    private final Set<String> changedClasses = new HashSet<>();
    private final Set<String> removedClasses = new HashSet<>();

    public Set<String> getAddedClasses() {
        return addedClasses;
    }

    public Set<String> getChangedClasses() {
        return changedClasses;
    }

    public Set<String> getRemovedClasses() {
        return removedClasses;
    }

    public Set<String> getAddedPackages() {
        return extractPackageNames(addedClasses);
    }

    public Set<String> getChangedPackages() {
        return extractPackageNames(changedClasses);
    }

    private Set<String> extractPackageNames(Set<String> classNames) {
        return classNames.stream()
                .map(name -> name.contains(".")
                        ? name.substring(0, name.lastIndexOf("."))
                        : name)
                .collect(Collectors.toSet());
    }
}