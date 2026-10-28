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

/**
 * Vaadin Flow server-side framework.
 * <p>
 * The module is open because the framework is reflected on by libraries that
 * predate the module system, such as Jackson, Atmosphere and session
 * serialization tools.
 */
open module com.vaadin.flow.server {
    // Types of these modules are part of the Flow API
    requires transitive jakarta.servlet;
    requires transitive org.jsoup;
    requires transitive org.slf4j;
    requires transitive tools.jackson.core;
    requires transitive tools.jackson.databind;
    requires transitive com.fasterxml.jackson.annotation;
    // java.beans for bean introspection
    requires transitive java.desktop;

    // The client-side push library
    requires com.vaadin.flow.push;
    requires atmosphere.runtime;

    // Provided by the environment when the related feature is used
    requires static jakarta.annotation;
    requires static license.checker;
    // Compile-time only annotations
    requires static org.jspecify;
    requires static osgi.cmpn;

    exports com.vaadin.experimental;
    exports com.vaadin.flow.component;
    exports com.vaadin.flow.component.clipboard;
    exports com.vaadin.flow.component.dependency;
    exports com.vaadin.flow.component.download;
    exports com.vaadin.flow.component.fullscreen;
    exports com.vaadin.flow.component.geolocation;
    exports com.vaadin.flow.component.internal;
    exports com.vaadin.flow.component.newtab;
    exports com.vaadin.flow.component.page;
    exports com.vaadin.flow.component.screenorientation;
    exports com.vaadin.flow.component.template;
    exports com.vaadin.flow.component.template.internal;
    exports com.vaadin.flow.component.trigger.internal;
    exports com.vaadin.flow.component.wakelock;
    exports com.vaadin.flow.component.webcomponent;
    exports com.vaadin.flow.component.webshare;
    exports com.vaadin.flow.di;
    exports com.vaadin.flow.dom;
    exports com.vaadin.flow.dom.impl;
    exports com.vaadin.flow.function;
    exports com.vaadin.flow.i18n;
    exports com.vaadin.flow.internal;
    exports com.vaadin.flow.internal.change;
    exports com.vaadin.flow.internal.hilla;
    exports com.vaadin.flow.internal.menu;
    exports com.vaadin.flow.internal.nodefeature;
    exports com.vaadin.flow.internal.springcsrf;
    exports com.vaadin.flow.internal.streams;
    exports com.vaadin.flow.js;
    exports com.vaadin.flow.router;
    exports com.vaadin.flow.router.internal;
    exports com.vaadin.flow.server;
    exports com.vaadin.flow.server.auth;
    exports com.vaadin.flow.server.communication;
    exports com.vaadin.flow.server.communication.rpc;
    exports com.vaadin.flow.server.communication.streaming;
    exports com.vaadin.flow.server.data;
    exports com.vaadin.flow.server.dau;
    exports com.vaadin.flow.server.menu;
    exports com.vaadin.flow.server.startup;
    exports com.vaadin.flow.server.streams;
    exports com.vaadin.flow.server.webcomponent;
    exports com.vaadin.flow.shared;
    exports com.vaadin.flow.shared.communication;
    exports com.vaadin.flow.shared.ui;
    exports com.vaadin.flow.shared.util;
    exports com.vaadin.flow.signals;
    exports com.vaadin.flow.signals.function;
    exports com.vaadin.flow.signals.impl;
    exports com.vaadin.flow.signals.local;
    exports com.vaadin.flow.signals.operations;
    exports com.vaadin.flow.signals.shared;
    exports com.vaadin.flow.signals.shared.impl;
    exports com.vaadin.flow.theme;

    uses com.vaadin.experimental.FeatureFlagProvider;
    uses com.vaadin.flow.di.Instantiator;
    uses com.vaadin.flow.server.VaadinServiceInitListener;
    uses com.vaadin.flow.server.communication.PushConnectionFactory;
    uses com.vaadin.flow.server.startup.NavigationTargetFilter;

    provides com.vaadin.experimental.FeatureFlagProvider
            with com.vaadin.experimental.CoreFeatureFlagProvider,
            com.vaadin.experimental.HillaFeatureFlagProvider,
            com.vaadin.experimental.FlowComponentsFeatureFlagProvider;
    provides jakarta.servlet.ServletContainerInitializer with
            com.vaadin.flow.server.startup.LookupServletContainerInitializer,
            com.vaadin.flow.server.startup.RouteRegistryInitializer,
            com.vaadin.flow.server.startup.ErrorNavigationTargetInitializer,
            com.vaadin.flow.server.startup.AnnotationValidator,
            com.vaadin.flow.server.startup.WebComponentExporterAwareValidator,
            com.vaadin.flow.server.startup.WebComponentConfigurationRegistryInitializer,
            com.vaadin.flow.server.startup.VaadinAppShellInitializer;
}
