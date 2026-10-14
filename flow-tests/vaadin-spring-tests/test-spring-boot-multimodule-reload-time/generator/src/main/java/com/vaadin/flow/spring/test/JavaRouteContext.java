/*
 * Copyright (C) 2000-2026 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
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
