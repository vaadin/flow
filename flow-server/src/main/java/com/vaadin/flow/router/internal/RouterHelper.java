/*
 * Copyright 2000-2018 Vaadin Ltd.
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

import com.vaadin.flow.router.Route;

/**
 * Helper class for router.
 *
 * @author Vaadin Ltd
 *
 */
public final class RouterHelper {

    private RouterHelper() {
    }

    /**
     * Gets the effective route path value of the annotated class.
     *
     * @param component
     *         the component where the route points to
     * @param route
     *         the annotation
     * @return The value of the annotation or naming convention based value if
     * no explicit value is given.
     */
    public static String resolve(Class<?> component, Route route) {
        if (route.value().equals(Route.NAMING_CONVENTION)) {
            String simpleName = component.getSimpleName();
            if ("MainView".equals(simpleName) || "Main".equals(simpleName)) {
                return "";
            }
            if (simpleName.endsWith("View")) {
                return simpleName
                        .substring(0, simpleName.length() - "View".length())
                        .toLowerCase();
            }
            return simpleName.toLowerCase();
        }
        return route.value();
    }
}
