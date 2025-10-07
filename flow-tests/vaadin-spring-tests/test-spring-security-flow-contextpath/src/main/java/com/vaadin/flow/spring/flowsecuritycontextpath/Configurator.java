package com.vaadin.flow.spring.flowsecuritycontextpath;

import com.vaadin.flow.component.dependency.StyleSheet;
import com.vaadin.flow.component.page.AppShellConfigurator;
import com.vaadin.flow.component.page.Push;
import com.vaadin.flow.server.PWA;
import com.vaadin.flow.theme.Theme;

@PWA(name = "Spring Security Flow Context Path Test Project", shortName = "SSH Test")
@Theme("spring-security-test-app")
@Push
@StyleSheet("@vaadin/aura/fake-aura.css")
public class Configurator implements AppShellConfigurator {

}
