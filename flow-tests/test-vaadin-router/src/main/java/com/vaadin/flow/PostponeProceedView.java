/*
 * Copyright (C) 2000-2026 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.NativeButton;
import com.vaadin.flow.component.html.NativeLabel;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.router.BeforeLeaveEvent;
import com.vaadin.flow.router.BeforeLeaveEvent.ContinueNavigationAction;
import com.vaadin.flow.router.BeforeLeaveObserver;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouterLink;
import com.vaadin.flow.uitest.servlet.ViewTestLayout;

@Route(value = "com.vaadin.flow.PostponeProceedView")
public class PostponeProceedView extends Div implements BeforeLeaveObserver {
    private ContinueNavigationAction continueNavigationAction;

    @Route(value = "com.vaadin.flow.PostponeProceedView.ProceedResultView", layout = ViewTestLayout.class)
    public static class ProceedResultView extends Div {
        public ProceedResultView() {
            NativeLabel label = new NativeLabel("Another view");
            label.setId("target");
            add(label);
        }
    }

    @Route(value = "com.vaadin.flow.PostponeProceedView.DelayedProceedTargetView", layout = ViewTestLayout.class)
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
