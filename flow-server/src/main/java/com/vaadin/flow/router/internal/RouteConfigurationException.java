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

public class RouteConfigurationException extends RuntimeException {

    private String existingPathPattern;

    private String pathPattern;

    private RouteTarget existingTarget;

    private RouteTarget target;


    public RouteConfigurationException(String message, String pathPattern, RouteTarget existingTarget, RouteTarget target) {
        this(message, pathPattern, pathPattern, existingTarget, target);
    }

    public RouteConfigurationException(String message, String existingPathPattern, String pathPattern, RouteTarget existingTarget, RouteTarget target) {
        super(message);
        this.existingPathPattern = existingPathPattern;
        this.pathPattern = pathPattern;
        this.existingTarget = existingTarget;
        this.target = target;
    }

    public String getExistingPathPattern() {
        return existingPathPattern;
    }

    public String getPathPattern() {
        return pathPattern;
    }

    public RouteTarget getExistingTarget() {
        return existingTarget;
    }

    public RouteTarget getTarget() {
        return target;
    }
}
