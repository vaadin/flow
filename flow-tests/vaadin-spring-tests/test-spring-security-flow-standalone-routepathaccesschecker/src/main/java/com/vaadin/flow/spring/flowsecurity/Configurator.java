package com.vaadin.flow.spring.flowsecurity;

import com.vaadin.flow.component.page.AppShellConfigurator;
import com.vaadin.flow.component.page.Push;
import com.vaadin.flow.theme.Theme;
import com.vaadin.flow.theme.lumo.Lumo;

@Theme(value = "spring-security-test-app", themeClass = Lumo.class)
@Push
public class Configurator implements AppShellConfigurator {

}
