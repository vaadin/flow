/*
 * Copyright 2000-2026 Vaadin Ltd.
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
package com.vaadin.flow.router;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import com.vaadin.flow.component.Component;

/**
 * Walks the route hierarchy starting from a given {@link Route @Route}
 * annotated class.
 * <p>
 * At each step, the walker first consults {@link RouteParent @RouteParent}: if
 * the annotation is present, its target is itself {@code @Route}-annotated and
 * has not yet been visited, that target becomes the parent. Otherwise the
 * walker falls back to URL-prefix walking: it takes the current class's route
 * template via {@link RouteConfiguration#getTemplate(Class)}, strips the last
 * {@code /}-separated segment, and looks up the resulting path in the supplied
 * {@link RouteConfiguration}. Traversal terminates when no parent can be
 * resolved, when a candidate would form a cycle, or when the current template
 * has no further segments to strip.
 * <p>
 * Cycles in the hierarchy are detected via a visited set and truncated rather
 * than reported as errors. The resulting chain is therefore always finite and
 * free of duplicates.
 *
 * @see Route
 * @see RouteParent
 * @see RouteConfiguration
 */
public final class RouteHierarchy {

    private RouteHierarchy() {
        // utility class - no instances
    }

    /**
     * Returns the chain of route classes from the conceptual root down to and
     * including {@code routeClass}.
     * <p>
     * The returned list is root-first: the root ancestor comes first and
     * {@code routeClass} is always the last element. When {@code routeClass} is
     * not annotated with {@link Route @Route}, an empty list is returned. When
     * {@code routeClass} is a {@code @Route} view but no parent can be
     * resolved, the returned list contains only {@code routeClass} itself.
     * <p>
     * The walk consults {@link RouteParent @RouteParent} first at each step and
     * falls back to URL-prefix walking via
     * {@link RouteConfiguration#getTemplate(Class)} and
     * {@link RouteConfiguration#getRoute(String)}. Cycles are truncated.
     *
     * @param routeClass
     *            the route class to start from, not {@code null}
     * @param routeConfiguration
     *            the route configuration used to resolve templates and ancestor
     *            routes, not {@code null}
     * @return root-first list of ancestor route classes inclusive of
     *         {@code routeClass}, or an empty list when {@code routeClass} is
     *         not annotated with {@link Route @Route}
     * @see RouteParent
     * @see RouteConfiguration
     */
    public static List<Class<? extends Component>> resolveAncestors(
            Class<? extends Component> routeClass,
            RouteConfiguration routeConfiguration) {
        Objects.requireNonNull(routeClass, "routeClass");
        Objects.requireNonNull(routeConfiguration, "routeConfiguration");
        if (!routeClass.isAnnotationPresent(Route.class)) {
            return Collections.emptyList();
        }
        List<Class<? extends Component>> chain = new ArrayList<>();
        Set<Class<? extends Component>> visited = new HashSet<>();
        Class<? extends Component> current = routeClass;
        chain.add(current);
        visited.add(current);

        while (true) {
            Optional<Class<? extends Component>> parent = findParent(current,
                    routeConfiguration, visited);
            if (parent.isEmpty()) {
                break;
            }
            Class<? extends Component> parentClass = parent.get();
            chain.add(parentClass);
            visited.add(parentClass);
            current = parentClass;
        }
        Collections.reverse(chain);
        return Collections.unmodifiableList(chain);
    }

    /**
     * Returns the immediate parent of {@code routeClass}, or an empty
     * {@link Optional} when no parent can be resolved.
     * <p>
     * This is equivalent to taking the second-to-last entry of
     * {@link #resolveAncestors(Class, RouteConfiguration)} for chains of length
     * two or more, and empty otherwise. {@link RouteParent @RouteParent} is
     * consulted first; URL-prefix walking is the fallback.
     *
     * @param routeClass
     *            the route class to find the parent of, not {@code null}
     * @param routeConfiguration
     *            the route configuration used to resolve templates and ancestor
     *            routes, not {@code null}
     * @return an {@link Optional} describing the parent route class, or
     *         {@link Optional#empty()} when {@code routeClass} has no
     *         {@link Route @Route} annotation or no parent can be resolved
     * @see RouteParent
     * @see RouteConfiguration
     */
    public static Optional<Class<? extends Component>> resolveParent(
            Class<? extends Component> routeClass,
            RouteConfiguration routeConfiguration) {
        Objects.requireNonNull(routeClass, "routeClass");
        Objects.requireNonNull(routeConfiguration, "routeConfiguration");
        if (!routeClass.isAnnotationPresent(Route.class)) {
            return Optional.empty();
        }
        Set<Class<? extends Component>> visited = new HashSet<>();
        visited.add(routeClass);
        return findParent(routeClass, routeConfiguration, visited);
    }

    /**
     * Resolves the parent of {@code current}, honouring {@link RouteParent}
     * first and URL-prefix walking second.
     *
     * @param current
     *            the class whose parent is being resolved
     * @param routeConfiguration
     *            the route configuration to consult
     * @param visited
     *            the set of classes already in the chain; candidates already
     *            present are rejected to avoid cycles
     * @return the parent class, or empty when no parent can be resolved
     */
    private static Optional<Class<? extends Component>> findParent(
            Class<? extends Component> current,
            RouteConfiguration routeConfiguration,
            Set<Class<? extends Component>> visited) {
        RouteParent annotation = current.getAnnotation(RouteParent.class);
        if (annotation != null) {
            Class<? extends Component> declared = annotation.value();
            if (declared.isAnnotationPresent(Route.class)
                    && !visited.contains(declared)) {
                return Optional.of(declared);
            }
            // Fall through to URL-prefix walking when @RouteParent is present
            // but its target is not @Route-annotated or would form a cycle.
        }
        Optional<String> template = routeConfiguration.getTemplate(current);
        if (template.isEmpty()) {
            return Optional.empty();
        }
        String currentTemplate = template.get();
        if (currentTemplate.isEmpty()) {
            // Already at the root URL - nothing to strip.
            return Optional.empty();
        }
        int lastSlash = currentTemplate.lastIndexOf('/');
        String stripped = (lastSlash >= 0)
                ? currentTemplate.substring(0, lastSlash)
                : "";
        Optional<Class<? extends Component>> candidate = routeConfiguration
                .getRoute(stripped);
        if (candidate.isPresent() && !visited.contains(candidate.get())) {
            return candidate;
        }
        return Optional.empty();
    }
}
