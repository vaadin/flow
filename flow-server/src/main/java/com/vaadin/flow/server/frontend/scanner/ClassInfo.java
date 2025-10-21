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
package com.vaadin.flow.server.frontend.scanner;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Holds information about a class and its frontend dependencies.
 * <p>
 * For internal use only. May be renamed or removed in a future release.
 */
public class ClassInfo {
    String className;
    final LinkedHashSet<String> modules = new LinkedHashSet<>();
    final LinkedHashSet<String> modulesDevelopmentOnly = new LinkedHashSet<>();
    final LinkedHashSet<String> scripts = new LinkedHashSet<>();
    final LinkedHashSet<String> scriptsDevelopmentOnly = new LinkedHashSet<>();
    final transient List<CssData> css = new ArrayList<>();
    String route = "";
    String layout;
    ThemeData theme = new ThemeData();
    Set<String> children = new LinkedHashSet<>();

    public ClassInfo(String className) {
        this.className = className;
    }
}
