package com.vaadin.flow.uitest.ui;

import com.vaadin.flow.component.page.AppShellConfigurator;
import com.vaadin.flow.server.PWA;
import com.vaadin.flow.theme.Theme;

@PWA(name = "Live Reload View", shortName = "live-reload-view")
@Theme("mytheme")
public class AppShell implements AppShellConfigurator {
}
