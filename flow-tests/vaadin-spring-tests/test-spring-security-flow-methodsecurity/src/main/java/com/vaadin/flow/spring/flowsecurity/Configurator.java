package com.vaadin.flow.spring.flowsecurity;

import com.vaadin.flow.component.dependency.StyleSheet;
import com.vaadin.flow.component.page.AppShellConfigurator;
import com.vaadin.flow.component.page.Push;
import com.vaadin.flow.server.PWA;
import com.vaadin.flow.theme.Theme;

@PWA(name = "Spring Security Helper Test Project", shortName = "SSH Test")
@Theme("spring-security-test-app")
@Push
@StyleSheet("@vaadin/aura/fake-aura.css")
public class Configurator implements AppShellConfigurator {

}
