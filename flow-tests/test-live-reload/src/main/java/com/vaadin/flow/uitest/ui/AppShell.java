package com.vaadin.flow.uitest.ui;

import com.vaadin.flow.component.page.AppShellConfigurator;
import com.vaadin.flow.server.PWA;
import com.vaadin.flow.theme.Theme;
import com.vaadin.flow.theme.lumo.Lumo;

@PWA(name = "Live Reload View", shortName = "live-reload-view")
@Theme(value = "mytheme", themeClass = Lumo.class)
public class AppShell implements AppShellConfigurator {
}
