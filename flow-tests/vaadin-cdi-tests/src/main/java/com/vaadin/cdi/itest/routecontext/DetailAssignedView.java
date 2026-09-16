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
import com.vaadin.flow.router.RouterLink;

@RouteScoped
@RouteScopeOwner(MasterView.class)
@Route(value = "assigned", layout = MasterView.class)
@CdiComponent
public class DetailAssignedView extends AbstractCountedView
        implements AfterNavigationObserver {

    public static final String MASTER = "master";
    public static final String BEAN_LABEL = "BEAN_LABEL";

    @Inject
    @RouteScopeOwner(MasterView.class)
    private AssignedBean assignedBean;
    private NativeLabel assignedLabel;

    @PostConstruct
    private void init() {
        assignedLabel = new NativeLabel();
        assignedLabel.setId(BEAN_LABEL);
        assignedBean.setData("ASSIGNED");
        add(assignedLabel, new Div(new RouterLink(MASTER, MasterView.class)));
    }

    @Override
    public void afterNavigation(AfterNavigationEvent event) {
        assignedLabel.setText(assignedBean.getData());
    }

}
