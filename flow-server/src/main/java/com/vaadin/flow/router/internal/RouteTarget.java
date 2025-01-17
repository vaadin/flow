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
package com.vaadin.flow.router.internal;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.internal.AnnotationReader;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouterLayout;

/**
 * Route target stores the target component and parent layouts.
 * <p>
 * For internal use only. May be renamed or removed in a future release.
 *
 * @since 1.0
 */
public class RouteTarget implements Serializable {

    private final Class<? extends Component> target;

    private final List<Class<? extends RouterLayout>> parentLayouts;

    private final boolean annotatedRoute;
    private final boolean registeredAtStartup;

    /**
     * Create a new Route target holder with the given target registered.
     *
     * @param target
     *            navigation target
     * @param parents
     *            parent layout chain
     */
    public RouteTarget(Class<? extends Component> target,
            List<Class<? extends RouterLayout>> parents) {
        this.target = target;
        this.parentLayouts = parents != null
                ? Collections.unmodifiableList(new ArrayList<>(parents))
                : Collections.emptyList();
        Route routeAnnotation = AnnotationReader
                .getAnnotationFor(target, Route.class).orElse(null);
        this.annotatedRoute = routeAnnotation != null;
        this.registeredAtStartup = routeAnnotation != null
                && routeAnnotation.registerAtStartup();
    }

    /**
     * Create a new Route target holder with the given target registered and
     * empty parent layouts.
     *
     * @param target
     *            navigation target
     */
    public RouteTarget(Class<? extends Component> target) {
        this(target, null);
    }

    /**
     * Get the component route target.
     *
     * @return component navigation target.
     */
    public Class<? extends Component> getTarget() {
        return target;
    }

    /**
     * Check if navigation target is present in current target.
     *
     * @param target
     *            navigation target to check for
     * @return true if navigation target is present in current target.
     */
    public boolean containsTarget(Class<? extends Component> target) {
        return Objects.equals(this.target, target);
    }

    /**
     * Get the parent layout chain.
     *
     * @return parent layout chain
     */
    public List<Class<? extends RouterLayout>> getParentLayouts() {
        return parentLayouts;
    }

    /**
     * Gets if the route navigation target is a {@link Route} annotated class or
     * not.
     *
     * @return {@literal true} if the navigation target class is annotated
     *         with @{@link Route} annotation, otherwise {@literal false}.
     */
    boolean isAnnotatedRoute() {
        return annotatedRoute;
    }

    /**
     * Gets if this route has been registered during the initial route
     * registration on application startup.
     * <p>
     *
     * @return {@literal true} if the route was registered at application
     *         startup, otherwise {@literal false}.
     */
    boolean isRegisteredAtStartup() {
        return registeredAtStartup;
    }

}
