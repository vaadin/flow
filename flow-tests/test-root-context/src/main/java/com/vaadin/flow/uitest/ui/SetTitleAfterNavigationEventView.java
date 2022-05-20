package com.vaadin.flow.uitest.ui;

import com.vaadin.flow.router.AfterNavigationEvent;
import com.vaadin.flow.router.AfterNavigationObserver;
import com.vaadin.flow.router.HasDynamicTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.uitest.servlet.ViewTestLayout;
import org.slf4j.LoggerFactory;

@Route(value = "com.vaadin.flow.uitest.ui.SetTitleAfterNavigationEventView", layout = ViewTestLayout.class)
public class SetTitleAfterNavigationEventView extends AbstractDivView
        implements HasDynamicTitle, AfterNavigationObserver {

    private String title = "my-initial-title";

    @Override
    public String getPageTitle() {
        LoggerFactory
                .getLogger(SetTitleAfterNavigationEventView.class.getName())
                .debug("HasDynamicTitle.getPageTitle() called. The current title value is = "
                        + title);
        return title;
    }

    @Override
    public void afterNavigation(AfterNavigationEvent event) {
        title = "my-changed-title-after-AfterNavigationEvent";
        LoggerFactory
                .getLogger(SetTitleAfterNavigationEventView.class.getName())
                .debug("AfterNavigationEvent listener called. The current title value is = "
                        + title);
    }
}