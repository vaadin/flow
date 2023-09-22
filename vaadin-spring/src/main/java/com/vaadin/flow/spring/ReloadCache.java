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
 * A class for holding development-time cached data. Static fields survive a
 * Spring Boot DevTools reload, so the cached data can be used after reload.
 */
class ReloadCache implements Serializable {
    static Set<Class<?>> appShellClasses;
    static Set<Class<?>> lookupClasses;
    static Set<String> validResources = new HashSet<>();
    static Set<String> skippedResources = new HashSet<>();
    static Set<String> dynamicWhiteList;
    static Set<String> routePackages;
    static Set<String> jarClassNames = new HashSet<>();
    static Set<Class<?>> jarClasses = new HashSet<>();
}