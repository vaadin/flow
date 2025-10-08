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
package com.vaadin.flow.uitest.ui;

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.NativeButton;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.BeforeLeaveEvent;
import com.vaadin.flow.router.BeforeLeaveEvent.ContinueNavigationAction;
import com.vaadin.flow.router.BeforeLeaveObserver;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouteParam;
import com.vaadin.flow.router.RouteParameters;
import com.vaadin.flow.router.RouterLink;
import com.vaadin.flow.uitest.servlet.ViewTestLayout;

@Route(value = "com.vaadin.flow.uitest.ui.PostponeUpdateView/:test?", layout = ViewTestLayout.class)
public class PostponeUpdateView extends Div
        implements BeforeLeaveObserver, BeforeEnterObserver {
    private ContinueNavigationAction continueNavigationAction;

    private int next = 1;
    private RouterLink link;
    private NativeButton proceedButton, cancelButton;

    public PostponeUpdateView() {
        link = new RouterLink();
        link.setText("initial");
        link.setId("link");
        link.setRoute(PostponeUpdateView.class,
                new RouteParameters(new RouteParam("test", next++)));
        add(link);

        proceedButton = new NativeButton("proceed", e -> {
            continueNavigationAction.proceed();
            cleanButtons();
        });
        proceedButton.setId("proceedButton");
        cancelButton = new NativeButton("cancel", e -> {
            continueNavigationAction.cancel();
            cleanButtons();
        });
        cancelButton.setId("cancelButton");
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        String parameter = event.getRouteParameters().get("test").orElse(null);
        if (parameter != null) {
            link.setText(parameter);
            if (Integer.parseInt(parameter) >= next) {
                next = Integer.parseInt(parameter) + 1;
            }
            link.setRoute(PostponeUpdateView.class,
                    new RouteParameters(new RouteParam("test", next++)));
        }
    }

    @Override
    public void beforeLeave(BeforeLeaveEvent event) {
        continueNavigationAction = event.postpone();

        add(proceedButton, cancelButton);
    }

    private void cleanButtons() {
        remove(proceedButton, cancelButton);
    }
}
