/*
 * Copyright 2000-2021 Vaadin Ltd.
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
package com.vaadin.cdi.itest.routecontext;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;

import com.vaadin.cdi.annotation.CdiComponent;
import com.vaadin.cdi.annotation.RouteScopeOwner;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.NativeLabel;
import com.vaadin.flow.router.Route;

/**
 * A plain sibling of {@link LayoutScopedPreservedView} under the same layout.
 * The layout instance is reused when navigating between the two, so the beans
 * it owns must not be recreated.
 */
@Route(value = "layout-scoped-plain", layout = MainLayout.class)
@CdiComponent
public class LayoutScopedPlainView extends Div {

    public static final String LAYOUT_BEAN_LABEL = "plain-layout-bean";

    @Inject
    @RouteScopeOwner(MainLayout.class)
    private Instance<MainLayoutBean> injection;

    @PostConstruct
    private void init() {
        NativeLabel beanLabel = new NativeLabel(injection.get().getData());
        beanLabel.setId(LAYOUT_BEAN_LABEL);
        add(beanLabel);
    }
}
