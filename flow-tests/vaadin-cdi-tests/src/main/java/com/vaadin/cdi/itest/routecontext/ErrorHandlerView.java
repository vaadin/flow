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

import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import jakarta.servlet.http.HttpServletResponse;

import com.vaadin.cdi.annotation.CdiComponent;
import com.vaadin.cdi.annotation.RouteScopeOwner;
import com.vaadin.cdi.annotation.RouteScoped;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.html.NativeButton;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.ErrorParameter;
import com.vaadin.flow.router.HasErrorParameter;
import com.vaadin.flow.router.ParentLayout;
import com.vaadin.flow.router.RouterLink;

@RouteScoped
@RouteScopeOwner(ErrorParentView.class)
@ParentLayout(ErrorParentView.class)
@CdiComponent
public class ErrorHandlerView extends AbstractCountedView
        implements HasErrorParameter<CustomException> {

    public static final String PARENT = "parent";

    @Inject
    @RouteScopeOwner(ErrorHandlerView.class)
    private Instance<CustomExceptionSubButton> buttonInjection;

    @Inject
    @RouteScopeOwner(ErrorHandlerView.class)
    private Instance<CustomExceptionSubDiv> divInjection;

    private boolean isSubDiv;

    private Component current;

    @Override
    public int setErrorParameter(BeforeEnterEvent event,
            ErrorParameter<CustomException> parameter) {
        add(new RouterLink(PARENT, ErrorParentView.class));

        NativeButton button = new NativeButton("switch content", ev -> {
            remove(current);
            if (isSubDiv) {
                current = buttonInjection.get();
            } else {
                current = divInjection.get();
            }
            add(current);
            isSubDiv = !isSubDiv;
        });
        button.setId("switch-content");
        add(button);
        current = buttonInjection.get();
        add(current);

        return HttpServletResponse.SC_INTERNAL_SERVER_ERROR;
    }

}
