/*
 * Copyright 2000-2019 Vaadin Ltd.
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
import com.vaadin.flow.component.html.Label;
import com.vaadin.flow.router.BeforeLeaveEvent;
import com.vaadin.flow.router.BeforeLeaveEvent.ContinueNavigationAction;
import com.vaadin.flow.router.BeforeLeaveObserver;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouterLink;
import com.vaadin.flow.uitest.servlet.ViewTestLayout;

@Route(value = "com.vaadin.flow.uitest.ui.PostponeProceedView", layout = ViewTestLayout.class)
public class PostponeProceedView extends Div implements BeforeLeaveObserver {

    @Route(value = "com.vaadin.flow.uitest.ui.PostponeProceedView.ProceedResultView", layout = ViewTestLayout.class)
    public static class ProceedResultView extends Div {
        public ProceedResultView() {
            Label label = new Label("Another view");
            label.setId("target");
            add(label);
        }
    }

    public PostponeProceedView() {
        RouterLink link = new RouterLink("Navigate to another view",
                ProceedResultView.class);
        link.setId("link");
        add(link);
    }

    @Override
    public void beforeLeave(BeforeLeaveEvent event) {
        ContinueNavigationAction action = event.postpone();
        action.proceed();
    }
}
