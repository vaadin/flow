package com.vaadin.flow.server.startup;

import com.vaadin.flow.function.DeploymentConfiguration;
import com.vaadin.flow.server.DevModeHandler;

public class DevModeInitializer {

    private static DevModeHandler devmodeHandler;

    public void start(DeploymentConfiguration configuration) {
        devmodeHandler = DevModeHandler.createInstance(configuration);
    }

    public static DevModeHandler getDevModeHandler() {
        return devmodeHandler;
    }
}
