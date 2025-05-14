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

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.NativeLabel;
import com.vaadin.flow.component.html.NativeButton;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.router.BeforeLeaveEvent;
import com.vaadin.flow.router.BeforeLeaveEvent.ContinueNavigationAction;
import com.vaadin.flow.router.BeforeLeaveObserver;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouterLink;
import com.vaadin.flow.uitest.servlet.ViewTestLayout;

@Route(value = "com.vaadin.flow.uitest.ui.PostponeProceedView", layout = ViewTestLayout.class)
public class PostponeProceedView extends Div implements BeforeLeaveObserver {
    private ContinueNavigationAction continueNavigationAction;

    @Route(value = "com.vaadin.flow.uitest.ui.PostponeProceedView.ProceedResultView", layout = ViewTestLayout.class)
    public static class ProceedResultView extends Div {
        public ProceedResultView() {
            NativeLabel label = new NativeLabel("Another view");
            label.setId("target");
            add(label);
        }
    }

    @Route(value = "com.vaadin.flow.uitest.ui.PostponeProceedView.DelayedProceedTargetView", layout = ViewTestLayout.class)
    public static class DelayedProceedTargetView extends Div {
        public DelayedProceedTargetView() {
            NativeLabel label = new NativeLabel("Delayed Proceed Target View");
            label.setId("target");
            add(label);
        }
    }

    public PostponeProceedView() {
        RouterLink link = new RouterLink("Navigate to another view",
                ProceedResultView.class);
        link.setId("link");
        add(link, new Paragraph());

        RouterLink delayedProceedLink = new RouterLink(
                "Navigate to another view with delayed proceed",
                DelayedProceedTargetView.class);
        delayedProceedLink.setId("delayedProceedLink");

        NativeButton postponedNavigateButton = new NativeButton(
                "Postponed navigate", event -> UI.getCurrent()
                        .navigate(DelayedProceedTargetView.class));
        postponedNavigateButton.setId("postponedNavigateButton");

        NativeButton proceedButton = new NativeButton("proceed",
                event -> continueNavigationAction.proceed());
        proceedButton.setId("proceedButton");

        add(delayedProceedLink, new Paragraph(), postponedNavigateButton,
                new Paragraph(), proceedButton);
    }

    @Override
    public void beforeLeave(BeforeLeaveEvent event) {
        if (event.getNavigationTarget() == ProceedResultView.class) {
            continueNavigationAction = event.postpone();
            continueNavigationAction.proceed();
        } else if (event
                .getNavigationTarget() == DelayedProceedTargetView.class) {
            continueNavigationAction = event.postpone();
        }
    }
}
