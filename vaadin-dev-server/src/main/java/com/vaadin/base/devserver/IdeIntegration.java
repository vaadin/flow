package com.vaadin.base.devserver;

import java.io.File;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.internal.ComponentTracker;
import com.vaadin.flow.internal.UsageStatistics;
import com.vaadin.flow.server.startup.ApplicationConfiguration;

/**
 * Integration with IDEs for development mode.
 */
public class IdeIntegration {

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

    private void internalShowInIde(Component component,
            StackTraceElement location) {
        if (location == null) {
            getLogger().error("Unable to find the location where the component "
                    + component.getClass().getName() + " was created");
            return;
        }

        String cls = location.getClassName();
        String filename = location.getFileName();

        if (cls.endsWith(filename.replace(".java", ""))) {
            File src = configuration.getJavaSourceFolder();
            File javaFile = new File(src, cls.replace(".", "/") + ".java");
            String absoluteFilename = javaFile.getAbsolutePath();
            if (!javaFile.exists()) {
                getLogger().error("Unable to find file in " + absoluteFilename);
                return;
            }

            if (!OpenInCurrentIde.openFile(javaFile,
                    location.getLineNumber())) {
                // Failed to open in IDE so print the file and line info.
                // Either an IDE makes it clickable or you can copy the file
                // info
                System.out.println(location);
            }
        } else {
            System.out.println(location);
        }
    }

    private static Logger getLogger() {
        return LoggerFactory.getLogger(IdeIntegration.class);
    }

}
