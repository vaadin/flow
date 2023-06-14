/*
 * Copyright 2000-2023 Vaadin Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.vaadin.base.devserver;

import java.io.File;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.internal.ComponentTracker;
import com.vaadin.flow.component.internal.ComponentTracker.Location;
import com.vaadin.flow.internal.UsageStatistics;
import com.vaadin.flow.server.startup.ApplicationConfiguration;

/**
 * Integration with IDEs for development mode.
 */
public final class IdeIntegration {

    private ApplicationConfiguration configuration;

    /**
     * Creates a new integration with the given configuration.
     */
    public IdeIntegration(ApplicationConfiguration configuration) {
        this.configuration = configuration;
        if (configuration.isProductionMode()) {
            getLogger().error(getClass().getSimpleName()
                    + " should never be created in production mode");
        }

    }

    /**
     * Opens, in the current IDE, the location (file + line number) where the
     * given component was created.
     *
     * @param component
     *            the component to show
     */
    public void showComponentCreateInIde(Component component) {
        UsageStatistics.markAsUsed("flow/showComponentCreateInIde", null);
        internalShowInIde(component, ComponentTracker.findCreate(component));
    }

    /**
     * Opens, in the current IDE, the location (file + line number) where the
     * given component was attached.
     *
     * @param component
     *            the component to show
     */
    public void showComponentAttachInIde(Component component) {
        UsageStatistics.markAsUsed("flow/showComponentAttachInIde", null);
        internalShowInIde(component, ComponentTracker.findAttach(component));
    }

    private void internalShowInIde(Component component, Location location) {
        if (location == null) {
            getLogger().error("Unable to find the location where the component "
                    + component.getClass().getName() + " was created");
            return;
        }
        File javaFile = location.findJavaFile(configuration);
        if (javaFile != null && !javaFile.exists()) {
            getLogger().error("Unable to find file in " + javaFile);
            return;
        }

        if (javaFile != null
                && OpenInCurrentIde.openFile(javaFile, location.lineNumber())) {
            return;
        }

        System.out.println(toStackTraceElement(location));
    }

    private StackTraceElement toStackTraceElement(Location location) {
        return new StackTraceElement("", "", "", location.className(),
                location.methodName(), location.filename(),
                location.lineNumber());

    }

    private static Logger getLogger() {
        return LoggerFactory.getLogger(IdeIntegration.class);
    }

}
