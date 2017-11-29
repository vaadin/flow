/*
 * Copyright 2000-2017 Vaadin Ltd.
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
package com.vaadin.router;

import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Collections;
import java.util.Deque;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.vaadin.server.startup.RouteRegistry;
import com.vaadin.ui.Component;

/**
 * Default implementation of the {@link RouteResolver} interface.
 *
 * @author Vaadin Ltd.
 */
public class DefaultRouteResolver implements RouteResolver {

    @Override
    public NavigationState resolve(ResolveRequest request) {
        RouteRegistry registry = request.getRouter().getRegistry();
        PathDetails path = findPathString(registry,
                request.getLocation().getSegments());
        if (path == null) {
            return null;
        }

        NavigationStateBuilder builder = new NavigationStateBuilder();
        Class<? extends Component> navigationTarget;
        try {
            if (!path.segments.isEmpty()) {
                navigationTarget = getNavigationTargetWithParameter(registry,
                        path.path, path.segments);
            } else {
                navigationTarget = getNavigationTarget(registry, path.path);
            }

            if (HasUrlParameter.class.isAssignableFrom(navigationTarget)) {
                List<String> pathParameters = getPathParameters(
                        request.getLocation().getPath(), path.path);
                if (!HasUrlParameter.verifyParameters(navigationTarget,
                        pathParameters)) {
                    return null;
                }
                builder.withTarget(navigationTarget, pathParameters);
            } else {
                builder.withTarget(navigationTarget);
            }
            builder.withPath(path.path);
        } catch (NotFoundException nfe) {
            String message = "Exception while navigation to path " + path;
            Logger.getLogger(this.getClass().getName()).log(Level.WARNING,
                    message, nfe);
            throw nfe;
        }

        return builder.build();
    }

    private static class PathDetails {
        private final String path;
        private final List<String> segments;

        /**
         * Constructor for path with segment details.
         * 
         * @param path
         *            path
         * @param segments
         *            segments for path
         */
        public PathDetails(String path, List<String> segments) {
            this.path = path;
            this.segments = segments;
        }
    }

    private PathDetails findPathString(RouteRegistry registry,
            List<String> pathSegments) {
        if (pathSegments.isEmpty()) {
            return null;
        }

        Deque<PathDetails> paths = new ArrayDeque<>();
        StringBuilder pathBuilder = new StringBuilder(pathSegments.get(0));
        if (!"".equals(pathSegments.get(0))) {
            paths.push(new PathDetails("", pathSegments));
        }
        for (int i = 0; i < pathSegments.size(); i++) {
            if (i != 0) {
                pathBuilder.append("/").append(pathSegments.get(i));
            }
            paths.push(new PathDetails(pathBuilder.toString(),
                    pathSegments.subList(i + 1, pathSegments.size())));
        }
        while (!paths.isEmpty()) {
            PathDetails pathDetails = paths.pop();
            Optional<?> target = registry.getNavigationTarget(pathDetails.path,
                    pathDetails.segments);
            if (target.isPresent()) {
                return pathDetails;
            }
        }
        return null;
    }

    private Class<? extends Component> getNavigationTarget(
            RouteRegistry registry, String path) throws NotFoundException {
        return registry.getNavigationTarget(path)
                .orElseThrow(() -> new NotFoundException(String.format(
                        "No navigation target found for path '%s'.", path)));
    }

    private Class<? extends Component> getNavigationTargetWithParameter(
            RouteRegistry registry, String path, List<String> segments)
            throws NotFoundException {
        return registry.getNavigationTarget(path, segments)
                .orElseThrow(() -> new NotFoundException(String.format(
                        "No navigation target found for path '%s'.", path)));
    }

    private List<String> getPathParameters(String completePath,
            String routePath) {
        assert completePath != null;
        assert routePath != null;

        String parameterPart = completePath.replaceFirst(routePath, "");
        if (parameterPart.startsWith("/")) {
            parameterPart = parameterPart.substring(1, parameterPart.length());
        }
        if (parameterPart.endsWith("/")) {
            parameterPart = parameterPart.substring(0,
                    parameterPart.length() - 1);
        }
        if (parameterPart.isEmpty()) {
            return Collections.emptyList();
        }
        return Arrays.asList(parameterPart.split("/"));
    }
}
