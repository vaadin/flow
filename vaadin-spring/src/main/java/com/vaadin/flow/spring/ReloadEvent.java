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
