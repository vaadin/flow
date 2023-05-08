package com.vaadin.flow.testnpmonlyfeatures.bytecodescanning;

import com.vaadin.flow.component.page.AppShellConfigurator;
import com.vaadin.flow.server.LoadDependenciesOnStartup;

@LoadDependenciesOnStartup(EagerViewWithLazyComponent.class)
public class AppConf implements AppShellConfigurator {

}
