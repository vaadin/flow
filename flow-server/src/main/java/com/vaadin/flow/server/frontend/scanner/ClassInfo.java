/*
 * Copyright (C) 2000-2026 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.server.frontend.scanner;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

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
