package com.vaadin.flow.server.frontend.scanner;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import com.vaadin.flow.router.Route;

/**
 * A simple container with the information related to an application
 * end-point, i.e. those classes annotated with the {@link Route}
 * annotation.
 */
public final class EndPointData implements Serializable {
    final String name;
    String route = "";
    String layout;
    ThemeData theme = new ThemeData();
    final HashSet<String> modules = new HashSet<>();
    final HashSet<String> scripts = new HashSet<>();
    final List<CssData> css = new ArrayList<>();

    private final HashSet<String> classes = new HashSet<>();

    EndPointData(Class<?> clazz) {
        this.name = clazz.getName();
    }

    // For debugging
    @Override
    public String toString() {
        return String.format(
                "%n view: %s%n route: %s%n%s%n layout: %s%n modules: %s%n scripts: %s%n css: %s%n",
                name, route, theme, layout, col2Str(modules),
                col2Str(scripts), col2Str(css));
    }

    Set<String> getModules() {
        return modules;
    }

    Set<String> getScripts() {
        return scripts;
    }

    Set<String> getClasses() {
        return classes;
    }

    ThemeData getTheme() {
        return theme;
    }

    String getRoute() {
        return route;
    }

    String getLayout() {
        return layout;
    }

    String getName() {
        return name;
    }

    Set<CssData> getCss() {
        return css.stream().collect(Collectors.toSet());
    }

    private String col2Str(Collection<?> s) {
        return String.join("\n          ", String.valueOf(s));
    }
}
