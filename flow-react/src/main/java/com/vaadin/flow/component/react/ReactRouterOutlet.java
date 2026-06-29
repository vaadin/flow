/*
 * Copyright (C) 2000-2026 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.component.react;

import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.dependency.JsModule;

/**
 * Component used to create a React {@code Outlet} element for binding a Hilla
 * React view inside a Flow view.
 *
 * @since 24.5
 */
@Tag("react-router-outlet")
@JsModule("./ReactRouterOutletElement.tsx")
public class ReactRouterOutlet extends ReactAdapterComponent {
}
