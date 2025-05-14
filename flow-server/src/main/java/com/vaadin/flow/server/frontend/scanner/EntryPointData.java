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
