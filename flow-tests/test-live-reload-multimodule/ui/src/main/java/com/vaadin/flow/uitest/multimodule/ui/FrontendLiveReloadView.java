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
