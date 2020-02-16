/*
 * Copyright 2000-2020 Vaadin Ltd.
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
 *
 */

package com.vaadin.flow.router.internal;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

import com.vaadin.flow.component.Component;

public class TestAbstractRouteRegistry extends AbstractRouteRegistry {

    @Override
    public RouteSearchResult getNavigationTargetResult(String path) {
        return getConfiguration().getRouteSearchResult(path);
    }

    @Override
    public Optional<Class<? extends Component>> getNavigationTarget(
            String pathString) {
        Objects.requireNonNull(pathString,
                "pathString must not be null.");
        return getConfiguration().getTarget(pathString);
    }

    @Override
    public Optional<Class<? extends Component>> getNavigationTarget(
            String pathString, List<String> segments) {

        return getNavigationTarget(
                PathUtil.getPath(pathString, segments));
    }

}
