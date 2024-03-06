/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.uitest.ui;

import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.html.NativeButton;
import com.vaadin.flow.internal.BrowserLiveReload;
import com.vaadin.flow.internal.BrowserLiveReloadAccess;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.VaadinService;
import com.vaadin.flow.uitest.servlet.ViewTestLayout;

@Route(value = "com.vaadin.flow.uitest.ui.JavaLiveReloadView", layout = ViewTestLayout.class)
public class JavaLiveReloadView extends AbstractLiveReloadView {
    public static final String JAVA_LIVE_RELOAD_TRIGGER_BUTTON = "java-live-reload-trigger-button";

    public JavaLiveReloadView() {
        NativeButton javaReloadButton = new NativeButton(
                "Trigger Java live reload");
        javaReloadButton.addClickListener(this::handleClickJavaLiveReload);
        javaReloadButton.setId(JAVA_LIVE_RELOAD_TRIGGER_BUTTON);
        add(javaReloadButton);
    }

    // Java triggered live reload is faked as we do not have Trava JDK in test
    private void handleClickJavaLiveReload(ClickEvent<?> event) {
        BrowserLiveReloadAccess liveReloadAccess = VaadinService.getCurrent()
                .getInstantiator().getOrCreate(BrowserLiveReloadAccess.class);
        BrowserLiveReload browserLiveReload = liveReloadAccess
                .getLiveReload(VaadinService.getCurrent());
        browserLiveReload.reload();
    }
}
