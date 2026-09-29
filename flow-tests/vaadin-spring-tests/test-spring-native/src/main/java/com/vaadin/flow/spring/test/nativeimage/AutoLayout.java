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
package com.vaadin.flow.spring.test.nativeimage;

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.router.Layout;
import com.vaadin.flow.router.RouterLayout;

/**
 * Applied to the routes under its path without the routes naming it, which the
 * router finds by scanning for {@link Layout} classes.
 */
@Layout("/auto-layout")
public class AutoLayout extends Div implements RouterLayout {

    public static final String LAYOUT_ID = "auto-layout";

    public AutoLayout() {
        Span layout = new Span("Automatic layout");
        layout.setId(LAYOUT_ID);
        add(layout);
    }
}
