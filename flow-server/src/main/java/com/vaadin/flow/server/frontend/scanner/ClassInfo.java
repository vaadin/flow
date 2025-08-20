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
    boolean loadCss = true;

    public ClassInfo(String className) {
        this.className = className;
    }
}
