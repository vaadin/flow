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

import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;

import com.vaadin.cdi.annotation.CdiComponent;
import com.vaadin.cdi.annotation.RouteScopeOwner;
import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.NativeLabel;
import com.vaadin.flow.router.PreserveOnRefresh;
import com.vaadin.flow.router.Route;

/**
 * A preserved child of {@link MainLayout}, which is not preserved itself. Flow
 * preserves the whole chain, so the beans owned by the layout are shared with
 * the UI created by a page refresh.
 */
@PreserveOnRefresh
@Route(value = "layout-scoped-preserved", layout = MainLayout.class)
@CdiComponent
public class LayoutScopedPreservedView extends Div {

    public static final String LAYOUT_BEAN_LABEL = "preserved-layout-bean";

    @Inject
    @RouteScopeOwner(MainLayout.class)
    private Instance<MainLayoutBean> injection;

    private NativeLabel beanLabel;

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        if (beanLabel != null) {
            remove(beanLabel);
        }
        beanLabel = new NativeLabel(injection.get().getData());
        beanLabel.setId(LAYOUT_BEAN_LABEL);
        add(beanLabel);
    }
}
