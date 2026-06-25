/*
 * Copyright (C) 2000-2026 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow;

import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.dependency.JsModule;
import com.vaadin.flow.component.react.ReactAdapterComponent;
import com.vaadin.flow.router.Route;

/**
 * Test view for vaadin/flow#20404 Set network to slow 4G and quickly click on
 * button. No console exceptions should be shown.
 */
@Route("com.vaadin.flow.ReactNavigateView")
@Tag("navigate-view")
@JsModule("NavigateView.tsx")
public class ReactNavigateView extends ReactAdapterComponent {

}
