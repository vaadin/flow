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
package com.vaadin.flow.spring.test;

import java.util.Collections;
import java.util.List;

public class JavaRouteContext extends JavaClassContext {
    String route;

    String styleName;

    List<JavaSpringServiceContext> services = Collections.emptyList();

    List<CssImportContext> cssImports = Collections.emptyList();

    List<JsModuleContext> jsModules = Collections.emptyList();

    public List<JavaSpringServiceContext> getServices() {
        return services;
    }

    public void setServices(List<JavaSpringServiceContext> services) {
        this.services = services != null ? services : Collections.emptyList();
    }

    public List<JsModuleContext> getJsModules() {
        return jsModules;
    }

    public void setJsModules(List<JsModuleContext> jsModules) {
        this.jsModules = jsModules != null ? jsModules
                : Collections.emptyList();
        ;
    }

    public String getStyleName() {
        return styleName;
    }

    public void setStyleName(String styleName) {
        this.styleName = styleName;
    }

    public String getRoute() {
        return route;
    }

    public void setRoute(String route) {
        this.route = route;
    }

    public List<CssImportContext> getCssImports() {
        return cssImports;
    }

    public void setCssImports(List<CssImportContext> cssImports) {
        this.cssImports = cssImports != null ? cssImports
                : Collections.emptyList();
    }
}
