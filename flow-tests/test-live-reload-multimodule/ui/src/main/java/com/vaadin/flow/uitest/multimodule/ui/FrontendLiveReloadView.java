/*
 * Copyright (C) 2000-2026 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.uitest.multimodule.ui;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.dependency.JsModule;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.uitest.servlet.ViewTestLayout;

@Route(value = "com.vaadin.flow.uitest.multimodule.ui.FrontendLiveReloadView", layout = ViewTestLayout.class)
public class FrontendLiveReloadView extends Div {

    @Tag("in-frontend")
    @JsModule("./in-frontend.js")
    public static class InFrontend extends Component {

    }

    @Tag("in-resources-frontend")
    @JsModule("./in-resources-frontend.js")
    public static class InResourcesFrontend extends Component {

    }

    public FrontendLiveReloadView() {
        add(new ProjectFolderInfo());
        add(new ProjectHotdeployInfo());
        add(new InFrontend());
        add(new InResourcesFrontend());
    }

}
