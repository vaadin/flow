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
package com.vaadin.flow.quarkus.it.routecontext;

import java.util.UUID;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import jakarta.servlet.http.HttpServletResponse;

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.NativeButton;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.ErrorParameter;
import com.vaadin.flow.router.HasErrorParameter;
import com.vaadin.flow.router.ParentLayout;
import com.vaadin.flow.router.RouterLink;
import com.vaadin.quarkus.annotation.NormalRouteScoped;
import com.vaadin.quarkus.annotation.RouteScopeOwner;
import com.vaadin.quarkus.annotation.RouteScoped;

@RouteScoped
@RouteScopeOwner(ErrorParentView.class)
@ParentLayout(ErrorParentView.class)
public class ErrorHandlerView extends AbstractCountedView
        implements HasErrorParameter<CustomException> {

    public static final String PARENT = "parent";

    @Inject
    @RouteScopeOwner(ErrorHandlerView.class)
    private Instance<ErrorBean1> bean1;

    @Inject
    @RouteScopeOwner(ErrorHandlerView.class)
    private Instance<ErrorBean2> bean2;

    private AbstractCountedBean current;

    @NormalRouteScoped
    @RouteScopeOwner(ErrorHandlerView.class)
    public static class ErrorBean1 extends AbstractCountedBean {

        @PostConstruct
        void init() {
            setData(UUID.randomUUID().toString());
        }
    }

    @NormalRouteScoped
    @RouteScopeOwner(ErrorHandlerView.class)
    public static class ErrorBean2 extends AbstractCountedBean {

        @PostConstruct
        void init() {
            setData(UUID.randomUUID().toString());
        }
    }

    @Override
    public int setErrorParameter(BeforeEnterEvent event,
            ErrorParameter<CustomException> parameter) {
        add(new RouterLink(PARENT, ErrorParentView.class));

        Div div = new Div();
        div.setId("bean-data");

        NativeButton button = new NativeButton("switch content", ev -> {
            if (current instanceof ErrorBean1) {
                current = bean2.get();
            } else {
                current = bean1.get();
            }
            div.setText(current.getData());
        });
        button.setId("switch-content");
        add(button);
        current = bean1.get();
        div.setText(current.getData());
        add(div);

        return HttpServletResponse.SC_INTERNAL_SERVER_ERROR;
    }

}
