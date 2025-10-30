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
