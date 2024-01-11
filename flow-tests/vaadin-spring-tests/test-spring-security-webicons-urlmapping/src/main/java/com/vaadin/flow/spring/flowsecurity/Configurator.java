package com.vaadin.flow.spring.flowsecurity;

import com.vaadin.flow.component.page.AppShellConfigurator;
import com.vaadin.flow.server.AppShellSettings;
import com.vaadin.flow.server.PWA;

import static com.vaadin.flow.spring.flowsecurity.Configurator.ICONS_PATH;

@PWA(name = "Spring Security Helper Test Project", shortName = "SSH Test", iconPath = ICONS_PATH
        + "hey.png")
public class Configurator implements AppShellConfigurator {

    public static final String ICONS_PATH = "custom/icons/path/";

    @Override
    public void configurePage(AppShellSettings settings) {
        settings.addFavIcon("icon", "/" + ICONS_PATH + "fav.ico", "32x32");
    }
}
