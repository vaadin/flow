/*
 * Copyright (C) 2000-2026 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.server.frontend.scanner;

import java.io.Serializable;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import com.vaadin.flow.component.WebComponentExporter;
import com.vaadin.flow.router.Route;

/**
 * A simple container with the information related to an application entry
 * point, i.e. those classes annotated with the {@link Route} annotation,
 * extending {@link WebComponentExporter} and a bunch of more internal classes.
 * <p>
 * For internal use only. May be renamed or removed in a future release.
 *
 * @since 2.0
 */
public final class EntryPointData implements Serializable {
    private final EntryPointType type;
    private final String name;
    private boolean eager = false;
    private List<String> dependencyTriggers;

    Set<String> reachableClasses;
    private LinkedHashSet<String> modules = new LinkedHashSet<>();
    private LinkedHashSet<String> modulesDevelopmentOnly = new LinkedHashSet<>();
    private LinkedHashSet<String> scripts = new LinkedHashSet<>();
    private LinkedHashSet<String> scriptsDevelopmentOnly = new LinkedHashSet<>();
    private LinkedHashSet<CssData> css = new LinkedHashSet<>();

    EntryPointData(Class<?> clazz, EntryPointType type,
            List<String> dependencyTriggers, boolean eager) {
        this.name = clazz.getName();
        this.type = type;
        this.dependencyTriggers = dependencyTriggers;
        this.eager = eager;
    }

    String getName() {
        return name;
    }

    public EntryPointType getType() {
        return type;
    }

    public LinkedHashSet<String> getModules() {
        return modules;
    }

    public LinkedHashSet<String> getModulesDevelopmentOnly() {
        return modulesDevelopmentOnly;
    }

    public Collection<String> getScripts() {
        return scripts;
    }

    public Collection<String> getScriptsDevelopmentOnly() {
        return scriptsDevelopmentOnly;
    }

    public Collection<CssData> getCss() {
        return css;
    }

    public void setDependencyTriggers(List<String> dependencyTriggers) {
        this.dependencyTriggers = dependencyTriggers;
    }

    public List<String> getDependencyTriggers() {
        return dependencyTriggers;
    }

    public boolean isEager() {
        return eager;
    }
}
