/*
 * Copyright 2000-2023 Vaadin Ltd.
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
}