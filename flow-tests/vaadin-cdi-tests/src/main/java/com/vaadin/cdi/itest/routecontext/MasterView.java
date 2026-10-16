/*
 * Copyright 2000-2018 Vaadin Ltd.
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
import jakarta.inject.Inject;

import com.vaadin.cdi.annotation.CdiComponent;
import com.vaadin.cdi.annotation.RouteScopeOwner;
import com.vaadin.cdi.annotation.RouteScoped;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.NativeLabel;
import com.vaadin.flow.router.AfterNavigationEvent;
import com.vaadin.flow.router.AfterNavigationObserver;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RoutePrefix;
import com.vaadin.flow.router.RouterLayout;
import com.vaadin.flow.router.RouterLink;

@RouteScoped
@Route("")
@RoutePrefix("master")
@CdiComponent
public class MasterView extends AbstractCountedView
        implements RouterLayout, AfterNavigationObserver {

    public static final String ASSIGNED = "assigned";
    public static final String ASSIGNED_BEAN_LABEL = "ASSIGNED";
    public static final String APART = "apart";
    public static final String APART_BEAN_LABEL = "APART";

    @Inject
    @RouteScopeOwner(MasterView.class)
    private AssignedBean assignedBean;
    private NativeLabel assignedLabel;

    @PostConstruct
    private void init() {
        assignedLabel = new NativeLabel();
        assignedLabel.setId(ASSIGNED_BEAN_LABEL);
        add(new NativeLabel("MASTER"), new Div(assignedLabel),
                new Div(new RouterLink(ASSIGNED, DetailAssignedView.class)),
                new Div(new RouterLink(APART, DetailApartView.class)));
    }

    @Override
    public void afterNavigation(AfterNavigationEvent event) {
        assignedLabel.setText(assignedBean.getData());
    }

}
